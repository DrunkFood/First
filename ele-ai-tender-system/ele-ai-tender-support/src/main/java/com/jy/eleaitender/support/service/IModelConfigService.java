package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.AiModelConfig;

public interface IModelConfigService {
    Page<AiModelConfig> getPage(Integer pageNum, Integer pageSize, String modelType, String usageScenario);
    AiModelConfig getById(Long id);
    AiModelConfig create(AiModelConfig config);
    void update(AiModelConfig config);
    void deleteById(Long id);
    void setActive(Long id, Integer isActive);
}
