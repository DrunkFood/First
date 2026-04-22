package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupTemplate;

public interface ITemplateConfigService {
    Page<SupTemplate> getPage(Integer pageNum, Integer pageSize, String templateName, String projectCategory, String projectType);
    SupTemplate getById(Long id);
    SupTemplate create(SupTemplate template);
    void update(SupTemplate template);
    void deleteById(Long id);
    void setDefault(Long id);
}
