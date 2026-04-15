package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.datascope.DataScopeHelper;
import com.jy.eleaitender.common.enums.ProjectPhase;
import com.jy.eleaitender.common.enums.ProjectStatus;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.dto.response.ProjectPhaseVO;
import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.core.mapper.AiProjectMapper;
import com.jy.eleaitender.core.service.IProjectService;
import com.jy.eleaitender.core.statemachine.ProjectStateMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 项目服务实现
 */
@Service
public class ProjectServiceImpl implements IProjectService {

    @Autowired
    private AiProjectMapper projectMapper;

    @Override
    public Page<AiProject> getPage(Integer pageNum, Integer pageSize, String projectName, String status, String projectCategory) {
        Page<AiProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<AiProject> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(projectName)) {
            wrapper.like(AiProject::getProjectName, projectName);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(AiProject::getStatus, status);
        }
        if (StringUtils.hasText(projectCategory)) {
            wrapper.eq(AiProject::getProjectCategory, projectCategory);
        }

        wrapper.orderByDesc(AiProject::getCreateTime);

        return projectMapper.selectPage(page, wrapper);
    }

    @Override
    public AiProject getById(Long id) {
        AiProject project = projectMapper.selectById(id);
        if (project == null) {
            throw new BusinessException(ResponseCode.PROJECT_NOT_FOUND);
        }
        DataScopeHelper.checkOwnership(project.getCreateId());
        return project;
    }

    @Override
    @Transactional
    public AiProject create(AiProject project) {
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
    public void update(Long id, AiProject project) {
        AiProject existing = getById(id); // 内部已做归属校验
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
            AiProject project = projectMapper.selectById(id);
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
        AiProject project = getById(projectId); // 内部已做归属校验
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
    public void advancePhase(Long projectId, Integer targetPhase) {
        AiProject project = getById(projectId); // 内部已做归属校验
        ProjectPhase target = ProjectPhase.fromCode(targetPhase);
        project.setCurrentPhase(target.getCode());
        project.setProgress(target.getProgressPercent());
        projectMapper.updateById(project);
    }

    @Override
    @Transactional
    public void changeStatus(Long projectId, String targetStatus) {
        AiProject project = getById(projectId); // 内部已做归属校验
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
        LambdaQueryWrapper<AiProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProject::getProjectCode, projectCode);
        if (excludeId != null) {
            wrapper.ne(AiProject::getId, excludeId);
        }
        if (projectMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResponseCode.PROJECT_EXISTS, "项目编号已存在: " + projectCode);
        }
    }

    private void validateProjectNameUnique(String projectName, Long excludeId) {
        LambdaQueryWrapper<AiProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiProject::getProjectName, projectName);
        if (excludeId != null) {
            wrapper.ne(AiProject::getId, excludeId);
        }
        if (projectMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ResponseCode.PROJECT_EXISTS, "项目名称已存在: " + projectName);
        }
    }
}
