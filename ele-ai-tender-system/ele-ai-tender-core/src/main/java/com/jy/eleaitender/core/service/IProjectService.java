package com.jy.eleaitender.core.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.core.dto.response.ProjectPhaseVO;
import com.jy.eleaitender.common.entity.core.TbProject;

import java.util.List;
import java.util.Map;

/**
 * 项目服务接口
 */
public interface IProjectService {

    /**
     * 分页查询项目列表
     */
    Page<TbProject> getPage(Integer pageNum, Integer pageSize, String projectName, String status, String projectCategory, String projectType);

    /**
     * 根据ID获取项目详情
     */
    TbProject getById(Long id);

    /**
     * 创建项目
     */
    TbProject create(TbProject project);

    /**
     * 更新项目
     */
    void update(Long id, TbProject project);

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
     *
     * @param projectId   项目ID
     * @param targetPhase 目标阶段编号
     * @param context     上下文参数（如policyFileIds等），可为null
     */
    void advancePhase(Long projectId, Integer targetPhase, Map<String, Object> context);

    /**
     * 提交AI生成需求
     */
    AiTask generateRequirement(Long projectId);

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

    /**
     * 校验项目名称是否唯一
     * @param projectName 项目名称
     * @param excludeId 排除的ID（编辑时排除自身）
     * @return true=名称可用，false=名称已存在
     */
    boolean checkNameUnique(String projectName, Long excludeId);
}
