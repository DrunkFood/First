package com.jy.eleaitender.ai.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jy.eleaitender.ai.mapper.AiModelConfigReadMapper;
import com.jy.eleaitender.ai.mapper.ModelRouteRuleReadMapper;
import com.jy.eleaitender.common.entity.ai.AiModelConfig;
import com.jy.eleaitender.common.entity.support.SupModelRouteRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 模型配置缓存服务
 * Redis缓存 + DB兜底，支持通过Redis Pub/Sub动态刷新
 */
@Slf4j
@Service
public class ModelConfigCacheService {

    private static final String ROUTE_CACHE_KEY = "ai:model:route:";
    private static final String CONFIG_CACHE_KEY = "ai:model:config:";
    private static final long CACHE_TTL_MINUTES = 30;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AiModelConfigReadMapper modelConfigMapper;

    @Autowired
    private ModelRouteRuleReadMapper routeRuleMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取指定场景的生效路由规则（优先缓存）
     *
     * @param scenario 使用场景 GENERATION/OPTIMIZATION/DETECTION
     * @return 按优先级排序的路由规则列表
     */
    public List<SupModelRouteRule> getActiveRouteRules(String scenario) {
        String cacheKey = ROUTE_CACHE_KEY + scenario;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, new TypeReference<List<SupModelRouteRule>>() {});
            }
        } catch (Exception e) {
            log.warn("读取路由规则缓存失败, scenario={}", scenario, e);
        }

        // 缓存未命中，查数据库
        LambdaQueryWrapper<SupModelRouteRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupModelRouteRule::getUsageScenario, scenario)
               .eq(SupModelRouteRule::getIsActive, 1)
               .eq(SupModelRouteRule::getIsDelete, 0)
               .orderByAsc(SupModelRouteRule::getPriority);
        List<SupModelRouteRule> rules = routeRuleMapper.selectList(wrapper);

        // 写入缓存
        try {
            String json = objectMapper.writeValueAsString(rules);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入路由规则缓存失败, scenario={}", scenario, e);
        }

        return rules;
    }

    /**
     * 获取模型配置详情（优先缓存）
     *
     * @param modelId 模型配置ID
     * @return 模型配置（不存在或已停用返回null）
     */
    public AiModelConfig getModelConfig(Long modelId) {
        if (modelId == null) {
            return null;
        }

        String cacheKey = CONFIG_CACHE_KEY + modelId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                if ("NULL".equals(cached)) {
                    return null;
                }
                return objectMapper.readValue(cached, AiModelConfig.class);
            }
        } catch (Exception e) {
            log.warn("读取模型配置缓存失败, modelId={}", modelId, e);
        }

        // 缓存未命中，查数据库
        AiModelConfig config = modelConfigMapper.selectById(modelId);

        // 写入缓存（不存在或已删除缓存NULL标记，防止缓存穿透）
        try {
            if (config == null || config.getIsDelete() != null && config.getIsDelete() == 1) {
                redisTemplate.opsForValue().set(cacheKey, "NULL", 5, TimeUnit.MINUTES);
                return null;
            }
            String json = objectMapper.writeValueAsString(config);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入模型配置缓存失败, modelId={}", modelId, e);
        }

        return config;
    }

    /**
     * 清除所有模型相关缓存（由刷新监听器调用）
     */
    public void evictAll() {
        try {
            var routeKeys = redisTemplate.keys(ROUTE_CACHE_KEY + "*");
            if (routeKeys != null && !routeKeys.isEmpty()) {
                redisTemplate.delete(routeKeys);
            }
            var configKeys = redisTemplate.keys(CONFIG_CACHE_KEY + "*");
            if (configKeys != null && !configKeys.isEmpty()) {
                redisTemplate.delete(configKeys);
            }
            log.info("模型配置缓存已清除");
        } catch (Exception e) {
            log.error("清除模型配置缓存失败", e);
        }
    }
}
