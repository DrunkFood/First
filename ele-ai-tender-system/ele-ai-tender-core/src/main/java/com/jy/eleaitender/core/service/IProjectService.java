package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.core.dto.response.ProjectPhaseVO;
import com.jy.eleaitender.common.entity.core.AiProject;

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

    /**
     * 获取项目阶段进度
     */
    ProjectPhaseVO getPhase(Long projectId);

    /**
     * 推进项目阶段
     */
    void advancePhase(Long projectId, Integer targetPhase);

    /**
     * 变更项目状态
     */
    void changeStatus(Long projectId, String targetStatus);

    /**
     * 取消项目
     */
    void cancelProject(Long projectId);

    /**
     * 发布项目
     */
    void publishProject(Long projectId);

    /**
     * 归档项目
     */
    void archiveProject(Long projectId);
}
