package com.jy.eleaitender.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiModelConfig;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.support.mapper.ModelConfigMapper;
import com.jy.eleaitender.support.service.IModelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ModelConfigServiceImpl implements IModelConfigService {

    @Autowired
    private ModelConfigMapper modelConfigMapper;

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
        return config;
    }

    @Override
    @Transactional
    public void update(AiModelConfig config) {
        modelConfigMapper.updateById(config);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        modelConfigMapper.deleteById(id);
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
    }
}
