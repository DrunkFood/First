package com.jy.eleaitender.core.statemachine;

import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.enums.ProjectStatus;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

/**
 * 项目状态机
 * 定义合法的状态转换规则
 */
@Slf4j
public class ProjectStateMachine {

    private static final Map<ProjectStatus, Set<ProjectStatus>> TRANSITIONS = Map.of(
            ProjectStatus.DRAFT, Set.of(ProjectStatus.IN_PROGRESS, ProjectStatus.CANCELLED),
            ProjectStatus.IN_PROGRESS, Set.of(ProjectStatus.PENDING_DETECTION, ProjectStatus.CANCELLED),
            ProjectStatus.PENDING_DETECTION, Set.of(ProjectStatus.DETECTING, ProjectStatus.IN_PROGRESS, ProjectStatus.CANCELLED),
            ProjectStatus.DETECTING, Set.of(ProjectStatus.DETECTION_PASSED, ProjectStatus.DETECTION_FAILED, ProjectStatus.DETECTION_SKIPPED),
            ProjectStatus.DETECTION_PASSED, Set.of(ProjectStatus.PUBLISHED, ProjectStatus.IN_PROGRESS),
            ProjectStatus.DETECTION_FAILED, Set.of(ProjectStatus.IN_PROGRESS, ProjectStatus.DETECTION_PASSED),
            ProjectStatus.DETECTION_SKIPPED, Set.of(ProjectStatus.PUBLISHED, ProjectStatus.IN_PROGRESS),
            ProjectStatus.PUBLISHED, Set.of(ProjectStatus.ARCHIVED),
            ProjectStatus.ARCHIVED, Set.of(),
            ProjectStatus.CANCELLED, Set.of()
    );

    /**
     * 执行状态转换
     *
     * @throws BusinessException 非法转换时抛出
     */
    public static void transition(TbProject project, ProjectStatus target) {
        ProjectStatus current = ProjectStatus.fromCode(project.getStatus());
        Set<ProjectStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new BusinessException(ResponseCode.PROJECT_STATUS_ERROR,
                    "不允许从[" + current.getLabel() + "]转为[" + target.getLabel() + "]");
        }
        project.setStatus(target.getCode());
    }

    /**
     * 检查状态转换是否合法
     */
    public static boolean canTransition(String currentStatusCode, ProjectStatus target) {
        try {
            ProjectStatus current = ProjectStatus.fromCode(currentStatusCode);
            return TRANSITIONS.getOrDefault(current, Set.of()).contains(target);
        } catch (Exception e) {
            log.warn("检查项目状态转换失败: currentStatusCode={}, target={}", currentStatusCode, target, e);
            return false;
        }
    }
}
