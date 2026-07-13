package com.jy.eleaitender.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jy.eleaitender.common.datascope.DataScopeHelper;
import com.jy.eleaitender.common.dto.ai.RequirementGenerateParams;
import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.enums.*;
import com.jy.eleaitender.common.exception.BusinessException;
import com.jy.eleaitender.core.dto.response.ProjectPhaseVO;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IProjectService;
import com.jy.eleaitender.core.statemachine.PhaseFlowController;
import com.jy.eleaitender.core.statemachine.ProjectStateMachine;
import com.jy.eleaitender.core.statemachine.trigger.*;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 项目服务实现
 */
@Slf4j
@Service
public class ProjectServiceImpl implements IProjectService {

    private static final String PROJECT_CODE_UNIQUE_KEY = "uk_project_code";

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
    public Page<TbProject> getPage(Integer pageNum, Integer pageSize, String projectName, String projectCode, String status, String projectCategory, String projectType, String createTimeStart, String createTimeEnd) {
        Page<TbProject> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TbProject> wrapper = new LambdaQueryWrapper<>();

        // 项目名称或编号模糊查询（OR 逻辑）
        boolean hasName = StringUtils.hasText(projectName);
        boolean hasCode = StringUtils.hasText(projectCode);
        if (hasName && hasCode) {
            wrapper.and(w -> w.like(TbProject::getProjectName, projectName).or().like(TbProject::getProjectCode, projectCode));
        } else if (hasName) {
            wrapper.like(TbProject::getProjectName, projectName);
        } else if (hasCode) {
            wrapper.like(TbProject::getProjectCode, projectCode);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(TbProject::getStatus, status);
        }
        if (StringUtils.hasText(projectCategory)) {
            wrapper.eq(TbProject::getProjectCategory, projectCategory);
        }
        if (StringUtils.hasText(projectType)) {
            wrapper.eq(TbProject::getProjectType, projectType);
        }
        if (StringUtils.hasText(createTimeStart)) {
            wrapper.ge(TbProject::getCreateTime, createTimeStart + " 00:00:00");
        }
        if (StringUtils.hasText(createTimeEnd)) {
            wrapper.le(TbProject::getCreateTime, createTimeEnd + " 23:59:59");
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
    @Transactional(rollbackFor = Exception.class)
    public TbProject create(TbProject project) {
        validateProjectRequiredFields(project);
        validateProjectCodeUnique(project.getProjectCode(), null);
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
        try {
            projectMapper.insert(project);
        } catch (DataIntegrityViolationException e) {
            if (isProjectCodeUniqueViolation(e)) {
                log.warn("项目编号重复: projectCode={}", project.getProjectCode(), e);
                throwProjectCodeExists(project.getProjectCode());
            }
            throw e;
        }
        return project;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TbProject project) {
        TbProject existing = getById(id); // 内部已做归属校验
        // 校验项目名称唯一性
        if (StringUtils.hasText(project.getProjectName())) {
            validateProjectNameUnique(project.getProjectName(), id);
        }
        project.setId(id);
        // 不允许修改项目编号
        project.setProjectCode(existing.getProjectCode());
        projectMapper.updateById(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
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

    private void validateProjectRequiredFields(TbProject project) {
        if (!StringUtils.hasText(project.getProjectCode())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "项目编号不能为空");
        }
        if (!StringUtils.hasText(project.getReviewType())) {
            throw new BusinessException(ResponseCode.PARAM_ERROR, "评审方式不能为空");
        }
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
            log.warn("项目阶段转换失败: projectId={}, currentPhase={}", projectId, project.getCurrentPhase(), e);
            vo.setCurrentPhaseName("未知");
        }
        try {
            ProjectStatus status = ProjectStatus.fromCode(project.getStatus());
            vo.setStatusName(status.getLabel());
        } catch (Exception e) {
            log.warn("项目状态转换失败: projectId={}, status={}", projectId, project.getStatus(), e);
            vo.setStatusName(project.getStatus());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void advancePhase(Long projectId, Integer targetPhase, Map<String, Object> context) {
        TbProject project = getById(projectId); // 内部已做归属校验
        ProjectPhase target = ProjectPhase.fromCode(targetPhase);
        phaseFlowController.advancePhase(project, target, context);
        projectMapper.updateById(project);
    }

    @Override
    public AiTask generateRequirement(Long projectId) {
        TbProject project = getById(projectId);
        RequirementGenerateParams params = new RequirementGenerateParams();
        params.setRequirementName(project.getProjectName() + " - 招标需求");
        params.setProjectType(project.getProjectType());
        params.setProjectCategory(project.getProjectCategory());
        params.setBudget(project.getBudget() != null ? project.getBudget().toPlainString() : "");
        params.setDescription(project.getProjectDescription());

        // 参考文档内容 从 自动匹配的第一份文件/手动选择匹配的历史文件id/上传的文件id 中获取
        StringJoiner paramJoiner = new StringJoiner(",");
        MatchMode matchMode = MatchMode.fromCode(project.getMatchMode());
        switch (matchMode) {
            case AUTO_MATCH:
            case MANUAL_SELECT:
                if (project.getMatchedFileId() != null) {
                    paramJoiner.add(String.valueOf(project.getMatchedFileId()));
                }
                break;
            case UPLOAD:
                if (project.getUploadedFileId() != null) {
                    paramJoiner.add(String.valueOf(project.getUploadedFileId()));
                }
                break;
        }
        return aiTaskService.createInternalTask(AiTaskType.PROJECT_REQUIREMENT_GENERATE,
                project.getId(), project.getId(), BizType.PROJECT.getCode(), params, paramJoiner.toString());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long projectId, String targetStatus) {
        TbProject project = getById(projectId); // 内部已做归属校验
        ProjectStatus target = ProjectStatus.fromCode(targetStatus);
        ProjectStateMachine.transition(project, target);
        projectMapper.updateById(project);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelProject(Long projectId) {
        changeStatus(projectId, ProjectStatus.CANCELLED.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishProject(Long projectId) {
        changeStatus(projectId, ProjectStatus.PUBLISHED.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveProject(Long projectId) {
        changeStatus(projectId, ProjectStatus.ARCHIVED.getCode());
    }

    private void validateProjectCodeUnique(String projectCode, Long excludeId) {
        if (projectMapper.countByProjectCodeIncludingDeleted(projectCode, excludeId) > 0) {
            throwProjectCodeExists(projectCode);
        }
    }

    private void throwProjectCodeExists(String projectCode) {
        throw new BusinessException(ResponseCode.PROJECT_EXISTS, "项目编号已存在，请更换项目编号: " + projectCode);
    }

    private boolean isProjectCodeUniqueViolation(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains(PROJECT_CODE_UNIQUE_KEY)
                    || (message.contains("project_code") && message.contains("Duplicate entry")))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private void validateProjectNameUnique(String projectName, Long excludeId) {
        if (!checkNameUnique(projectName, excludeId)) {
            throw new BusinessException(ResponseCode.PROJECT_EXISTS, "项目名称已存在: " + projectName);
        }
    }

    @Override
    public boolean checkNameUnique(String projectName, Long excludeId) {
        if (!StringUtils.hasText(projectName)) {
            return true;
        }
        LambdaQueryWrapper<TbProject> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbProject::getProjectName, projectName);
        if (excludeId != null) {
            wrapper.ne(TbProject::getId, excludeId);
        }
        return projectMapper.selectCount(wrapper) == 0;
    }
}
