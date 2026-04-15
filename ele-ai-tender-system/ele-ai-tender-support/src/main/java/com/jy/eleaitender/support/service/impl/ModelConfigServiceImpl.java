package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.AiModelConfig;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.ModelConfigMapper;
import com.jy.eleaitender.support.service.IModelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ModelConfigServiceImpl implements IModelConfigService {

    private static final String CONFIG_REFRESH_CHANNEL = "ai:config:refresh";

    @Autowired
    private ModelConfigMapper modelConfigMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    public Page<AiModelConfig> getPage(Integer pageNum, Integer pageSize, String modelType, String usageScenario) {
        Page<AiModelConfig> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiModelConfig> wrapper = new LambdaQueryWrapper<>();
        
        if (StringUtils.hasText(modelType)) {
            wrapper.eq(AiModelConfig::getModelType, modelType);
        }
        if (StringUtils.hasText(usageScenario)) {
            wrapper.eq(AiModelConfig::getUsageScenario, usageScenario);
        }
        wrapper.eq(AiModelConfig::getIsDelete, 0);
        wrapper.orderByDesc(AiModelConfig::getCreateTime);
        
        return modelConfigMapper.selectPage(page, wrapper);
    }

    @Override
    public AiModelConfig getById(Long id) {
        return modelConfigMapper.selectById(id);
    }

    @Override
    @Transactional
    public AiModelConfig create(AiModelConfig config) {
        if (config.getIsActive() == null) {
            config.setIsActive(1);
        }
        if (config.getTokenUsage() == null) {
            config.setTokenUsage(0L);
        }
        modelConfigMapper.insert(config);
        publishConfigRefresh();
        return config;
    }

    @Override
    @Transactional
    public void update(AiModelConfig config) {
        modelConfigMapper.updateById(config);
        publishConfigRefresh();
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        modelConfigMapper.deleteById(id);
        publishConfigRefresh();
    }

    @Override
    @Transactional
    public void setActive(Long id, Integer isActive) {
        AiModelConfig config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(ResponseCode.MODEL_CONFIG_NOT_FOUND);
        }
        config.setIsActive(isActive);
        modelConfigMapper.updateById(config);
        publishConfigRefresh();
    }

    /**
     * 发布模型配置刷新通知到Redis Pub/Sub
     */
    private void publishConfigRefresh() {
        try {
            redisTemplate.convertAndSend(CONFIG_REFRESH_CHANNEL, "model_config_updated");
        } catch (Exception e) {
            // 通知失败不影响主流程，缓存会自然过期
        }
    }
}
