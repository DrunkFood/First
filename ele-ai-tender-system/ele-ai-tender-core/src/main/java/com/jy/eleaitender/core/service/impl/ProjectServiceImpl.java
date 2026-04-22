package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.datascope.DataScopeHelper;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.common.enums.ProjectPhase;
import com.jy.eleaitender.common.enums.ProjectStatus;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.dto.response.ProjectPhaseVO;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IProjectService;
import com.jy.eleaitender.core.statemachine.PhaseFlowController;
import com.jy.eleaitender.core.statemachine.ProjectStateMachine;
import com.jy.eleaitender.core.statemachine.trigger.BasicInfoTrigger;
import com.jy.eleaitender.core.statemachine.trigger.DetectionPhaseTrigger;
import com.jy.eleaitender.core.statemachine.trigger.DocumentTrigger;
import com.jy.eleaitender.core.statemachine.trigger.RequirementTrigger;
import com.jy.eleaitender.core.statemachine.trigger.ReviewItemTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import jakarta.annotation.PostConstruct;

/**
 * 项目服务实现
 */
@Service
public class ProjectServiceImpl implements IProjectService {

    @Autowired
    private TbProjectMapper projectMapper;

    @Autowired
    private IAiTaskService aiTaskService;

    @Autowired
    private PhaseFlowController phaseFlowController;

    @Lazy
    @Autowired
    private BasicInfoTrigger basicInfoTrigger;

    @Lazy
    @Autowired
    private RequirementTrigger requirementTrigger;

    @Lazy
    @Autowired
    private ReviewItemTrigger reviewItemTrigger;

    @Lazy
    @Autowired
    private DocumentTrigger documentTrigger;

    @Lazy
    @Autowired
    private DetectionPhaseTrigger detectionPhaseTrigger;

    /**
     * 注册所有阶段触发器
     */
    @PostConstruct
    public void initPhaseTriggers() {
        phaseFlowController.registerTrigger(ProjectPhase.BASIC_INFO, basicInfoTrigger);
        phaseFlowController.registerTrigger(ProjectPhase.REQUIREMENT, requirementTrigger);
        phaseFlowController.registerTrigger(ProjectPhase.REVIEW_ITEM, reviewItemTrigger);
        phaseFlowController.registerTrigger(ProjectPhase.DOCUMENT, documentTrigger);
        phaseFlowController.registerTrigger(ProjectPhase.DETECTION, detectionPhaseTrigger);
    }

    @Override
    public Page<TbProject> getPage(Integer pageNum, Integer pageSize, String projectName, String status, String projectCategory) {
        Page<TbProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TbProject> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(projectName)) {
            wrapper.like(TbProject::getProjectName, projectName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TbProject::getStatus, status);
        }
        if (StringUtils.hasText(projectCategory)) {
            wrapper.eq(TbProject::getProjectCategory, projectCategory);
        }

        wrapper.orderByDesc(TbProject::getCreateTime);

        return projectMapper.selectPage(page, wrapper);
    }

    @Override
    public TbProject getById(Long id) {
        TbProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }
        DataScopeHelper.checkOwnership(project.getCreateId());
        return project;
    }

    @Override
    @Transactional
    public TbProject create(TbProject project) {
        // 校验项目编号唯一性（如果用户提供了编号）
        if (StringUtils.hasText(project.getProjectCode())) {
            validateProjectCodeUnique(project.getProjectCode(), null);
        } else {
            project.setProjectCode(generateProjectCode());
        }
        // 校验项目名称唯一性
        if (StringUtils.hasText(project.getProjectName())) {
            validateProjectNameUnique(project.getProjectName(), null);
        }
        // 初始化状态为草稿
        if (!StringUtils.hasText(project.getStatus())) {
            project.setStatus(ProjectStatus.DRAFT.getCode());
        }
        // 初始化阶段和进度
        if (project.getCurrentPhase() == null) {
            project.setCurrentPhase(ProjectPhase.BASIC_INFO.getCode());
        }
        if (project.getProgress() == null) {
            project.setProgress(0);
        }
        projectMapper.insert(project);
        return project;
    }

    @Override
    @Transactional
    public void update(Long id, TbProject project) {
        TbProject existing = getById(id); // 内部已做归属校验
        project.setId(id);
        // 不允许修改项目编号
        project.setProjectCode(existing.getProjectCode());
        projectMapper.updateById(project);
    }

    @Override
    @Transactional
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "请选择要删除的项目");
        }
        for (Long id : ids) {
            TbProject project = projectMapper.selectById(id);
            if (project != null) {
                DataScopeHelper.checkOwnership(project.getCreateId());
            }
            projectMapper.deleteById(id);
        }
    }

    @Override
    public String generateProjectCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int randomDigits = ThreadLocalRandom.current().nextInt(100000, 999999);
        return "AI-" + timestamp + "-" + randomDigits;
    }

    @Override
    public ProjectPhaseVO getPhase(Long projectId) {
        TbProject project = getById(projectId); // 内部已做归属校验
        ProjectPhaseVO vo = new ProjectPhaseVO();
        vo.setProjectId(projectId);
        vo.setCurrentPhase(project.getCurrentPhase());
        vo.setProgress(project.getProgress());
        vo.setStatus(project.getStatus());
        try {
            ProjectPhase phase = ProjectPhase.fromCode(project.getCurrentPhase());
            vo.setCurrentPhaseName(phase.getLabel());
        } catch (Exception e) {
            vo.setCurrentPhaseName("未知");
        }
        try {
            ProjectStatus status = ProjectStatus.fromCode(project.getStatus());
            vo.setStatusName(status.getLabel());
        } catch (Exception e) {
            vo.setStatusName(project.getStatus());
        }
        return vo;
    }

    @Override
    @Transactional
    public void advancePhase(Long projectId, Integer targetPhase, Map<String, Object> context) {
        TbProject project = getById(projectId); // 内部已做归属校验
        ProjectPhase target = ProjectPhase.fromCode(targetPhase);
        phaseFlowController.advancePhase(project, target, context);
        projectMapper.updateById(project);
    }

    @Override
    public AiTask generateRequirement(Long projectId) {
        TbProject project = getById(projectId);
        Map<String, Object> params = new HashMap<>();
        params.put("requirementName", project.getProjectName() + " - 招标需求");
        params.put("projectType", project.getProjectType());
        params.put("projectCategory", project.getProjectCategory());
        params.put("budget", project.getBudget());
        params.put("description", project.getProjectDescription());
        // TODO 参考文档内容 从 自动匹配的第一份文件/手动选择匹配的历史文件id/上传的文件id 中获取
        params.put("referenceContent", "");
        return aiTaskService.createTask(AiTaskType.PROJECT_REQUIREMENT_GENERATE,
                project.getId(), project.getId(), "REQUIREMENT", params, null);
    }

    @Override
    @Transactional
    public void changeStatus(Long projectId, String targetStatus) {
        TbProject project = getById(projectId); // 内部已做归属校验
        ProjectStatus target = ProjectStatus.fromCode(targetStatus);
        ProjectStateMachine.transition(project, target);
        projectMapper.updateById(project);
    }

    @Override
    @Transactional
    public void cancelProject(Long projectId) {
        changeStatus(projectId, ProjectStatus.CANCELLED.getCode());
    }

    @Override
    @Transactional
    public void publishProject(Long projectId) {
        changeStatus(projectId, ProjectStatus.PUBLISHED.getCode());
    }

    @Override
    @Transactional
    public void archiveProject(Long projectId) {
        changeStatus(projectId, ProjectStatus.ARCHIVED.getCode());
    }

    private void validateProjectCodeUnique(String projectCode, Long excludeId) {
        LambdaQueryWrapper<TbProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbProject::getProjectCode, projectCode);
        if (excludeId != null) {
            wrapper.ne(TbProject::getId, excludeId);
        }
        if (projectMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResponseCode.PROJECT_EXISTS, "项目编号已存在: " + projectCode);
        }
    }

    private void validateProjectNameUnique(String projectName, Long excludeId) {
        LambdaQueryWrapper<TbProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbProject::getProjectName, projectName);
        if (excludeId != null) {
            wrapper.ne(TbProject::getId, excludeId);
        }
        if (projectMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResponseCode.PROJECT_EXISTS, "项目名称已存在: " + projectName);
        }
    }
}
