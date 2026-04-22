package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.support.SupTemplate;

import java.util.List;

/**
 * 模板服务接口
 */
public interface ITemplateService {

    /**
     * 分页查询模板列表
     */
    Page<SupTemplate> getPage(Integer pageNum, Integer pageSize, String templateName, String projectCategory, String projectType);

    /**
     * 根据ID获取模板详情
     */
    SupTemplate getById(Long id);

    /**
     * 获取默认模板
     */
    SupTemplate getDefault(String projectCategory, String projectType);

    /**
     * 创建模板
     */
    SupTemplate create(SupTemplate template);

    /**
     * 更新模板
     */
    void update(Long id, SupTemplate template);

    /**
     * 批量删除模板
     */
    void deleteByIds(List<Long> ids);

    /**
     * 设为默认模板
     */
    void setDefault(Long id);
}
