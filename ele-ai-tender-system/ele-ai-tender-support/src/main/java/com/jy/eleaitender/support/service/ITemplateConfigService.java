package com.jy.eleaitender.support.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jy.eleaitender.common.entity.support.SupTemplate;

public interface ITemplateConfigService extends IService<SupTemplate> {
    Page<SupTemplate> getPage(Integer pageNum, Integer pageSize, String templateName, String projectCategory, String projectType, String status);
    SupTemplate getById(Long id);
    SupTemplate create(SupTemplate template);
    void update(SupTemplate template);
    void deleteById(Long id);
    void setDefault(Long id);
    void setStatus(Long id, String status);
}
