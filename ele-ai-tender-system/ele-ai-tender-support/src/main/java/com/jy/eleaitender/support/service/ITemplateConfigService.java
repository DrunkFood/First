package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiTemplate;

public interface ITemplateConfigService {
    Page<AiTemplate> getPage(Integer pageNum, Integer pageSize, String templateName, String projectCategory, String projectType);
    AiTemplate getById(Long id);
    AiTemplate create(AiTemplate template);
    void update(AiTemplate template);
    void deleteById(Long id);
    void setDefault(Long id);
}
