package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.core.entity.AiProject;

import java.util.List;

/**
 * 项目服务接口
 */
public interface IProjectService {

    /**
     * 分页查询项目列表
     */
    Page<AiProject> getPage(Integer pageNum, Integer pageSize, String projectName, String status, String projectCategory);

    /**
     * 根据ID获取项目详情
     */
    AiProject getById(Long id);

    /**
     * 创建项目
     */
    AiProject create(AiProject project);

    /**
     * 更新项目
     */
    void update(Long id, AiProject project);

    /**
     * 批量删除项目
     */
    void deleteByIds(List<Long> ids);

    /**
     * 生成项目编号
     */
    String generateProjectCode();
}
