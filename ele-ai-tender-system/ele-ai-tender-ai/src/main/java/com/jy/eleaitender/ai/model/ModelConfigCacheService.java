package com.jy.eleaitender.ai.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jy.eleaitender.ai.mapper.AiModelConfigReadMapper;
import com.jy.eleaitender.ai.mapper.ModelRouteRuleReadMapper;
import com.jy.eleaitender.common.entity.ai.AiModelConfig;
import com.jy.eleaitender.common.entity.support.SupModelRouteRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 模型配置查询服务
 * 直接查DB，不缓存，保证配置实时生效
 */
@Slf4j
@Service
public class ModelConfigCacheService {

    @Autowired
    private AiModelConfigReadMapper modelConfigMapper;

    @Autowired
    private ModelRouteRuleReadMapper routeRuleMapper;

    /**
     * 获取指定场景的生效路由规则
     *
     * @param scenario 使用场景 GENERATION/OPTIMIZATION/DETECTION
     * @return 按优先级排序的路由规则列表
     */
    public List<SupModelRouteRule> getActiveRouteRules(String scenario) {
        LambdaQueryWrapper<SupModelRouteRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SupModelRouteRule::getUsageScenario, scenario)
               .eq(SupModelRouteRule::getIsActive, 1)
               .eq(SupModelRouteRule::getIsDelete, 0)
               .orderByAsc(SupModelRouteRule::getPriority);
        return routeRuleMapper.selectList(wrapper);
    }

    /**
     * 获取模型配置详情
     *
     * @param modelId 模型配置ID
     * @return 模型配置（不存在或已删除返回null）
     */
    public AiModelConfig getModelConfig(Long modelId) {
        if (modelId == null) {
            return null;
        }
        AiModelConfig config = modelConfigMapper.selectById(modelId);
        if (config == null || (config.getIsDelete() != null && config.getIsDelete() == 1)) {
            return null;
        }
        return config;
    }
}
