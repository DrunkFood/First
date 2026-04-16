package com.jy.eleaitender.core.statemachine;

import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.enums.ProjectPhase;
import com.jy.eleaitender.common.enums.ProjectStatus;
import com.jy.eleaitender.common.enums.ResponseCode;
import com.jy.eleaitender.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 阶段流程控制器
 * 管理编制阶段的推进规则、触发器注册、Phase-Status联动
 */
@Slf4j
@Component
public class PhaseFlowController {

    private final Map<ProjectPhase, PhaseTrigger> triggers = new EnumMap<>(ProjectPhase.class);

    /**
     * Phase → Status 联动映射
     * 进入某阶段时，项目状态应同步更新
     */
    private static final Map<ProjectPhase, ProjectStatus> PHASE_STATUS_MAPPING = Map.of(
            ProjectPhase.BASIC_INFO, ProjectStatus.IN_PROGRESS,
            ProjectPhase.REQUIREMENT, ProjectStatus.IN_PROGRESS,
            ProjectPhase.REVIEW_ITEM, ProjectStatus.IN_PROGRESS,
            ProjectPhase.DOCUMENT, ProjectStatus.IN_PROGRESS,
            ProjectPhase.DETECTION, ProjectStatus.DETECTING
    );

    /**
     * 注册阶段触发器
     */
    public void registerTrigger(ProjectPhase phase, PhaseTrigger trigger) {
        triggers.put(phase, trigger);
        log.info("注册阶段触发器: {} -> {}", phase.getLabel(), trigger.getClass().getSimpleName());
    }

    /**
     * 推进项目阶段
     * 流程: 校验转换规则 → 执行当前阶段onExit → 更新阶段 → 执行目标阶段onEnter → 联动Status
     *
     * @param project 项目实体
     * @param target  目标阶段
     * @param context 上下文参数（如policyFileIds等），可为null
     */
    public void advancePhase(AiProject project, ProjectPhase target, Map<String, Object> context) {
        ProjectPhase current = ProjectPhase.fromCode(project.getCurrentPhase());

        // 1. 校验转换规则（只能推进到下一阶段，不能跳跃）
        validateTransition(current, target);

        // 2. 执行当前阶段的 onExit（离开前校验是否可完成）
        PhaseTrigger currentTrigger = triggers.get(current);
        if (currentTrigger != null) {
            if (!currentTrigger.canComplete(project)) {
                throw new BusinessException(ResponseCode.PROJECT_PHASE_ERROR,
                        currentTrigger.getIncompleteMessage());
            }
            currentTrigger.onExit(project);
        }

        // 3. 更新阶段和进度
        project.setCurrentPhase(target.getCode());
        project.setProgress(target.getProgressPercent());

        // 4. 执行目标阶段的 onEnter（自动发起AI任务等）
        PhaseTrigger targetTrigger = triggers.get(target);
        if (targetTrigger != null) {
            targetTrigger.onEnter(project, context);
        }

        // 5. 联动更新项目状态
        syncProjectStatus(project, target);

        log.info("项目阶段推进: projectId={}, {}→{}", project.getId(),
                current.getLabel(), target.getLabel());
    }

    /**
     * 检查是否可以推进到目标阶段
     */
    public boolean canAdvance(AiProject project, ProjectPhase target) {
        ProjectPhase current = ProjectPhase.fromCode(project.getCurrentPhase());
        if (!isValidTransition(current, target)) {
            return false;
        }
        PhaseTrigger currentTrigger = triggers.get(current);
        return currentTrigger == null || currentTrigger.canComplete(project);
    }

    /**
     * 校验阶段转换是否合法（只能推进到下一阶段）
     */
    private void validateTransition(ProjectPhase current, ProjectPhase target) {
        if (!isValidTransition(current, target)) {
            throw new BusinessException(ResponseCode.PROJECT_PHASE_ERROR,
                    "不允许从[" + current.getLabel() + "]跳转到[" + target.getLabel() + "]，只能顺序推进");
        }
    }

    /**
     * 判断阶段转换是否合法
     * 规则：只能推进到下一相邻阶段，不能跳跃也不能倒退
     */
    private boolean isValidTransition(ProjectPhase current, ProjectPhase target) {
        return target.getCode() == current.getCode() + 1;
    }

    /**
     * 联动更新项目状态
     * - DRAFT → IN_PROGRESS：首次进入编制阶段
     * - 进入 DETECTION 阶段：若状态已是 DETECTING（由 onEnter 触发器提交检测），跳过
     * - 进入其他阶段：若当前是 DRAFT，流转到 IN_PROGRESS
     */
    private void syncProjectStatus(AiProject project, ProjectPhase target) {
        ProjectStatus expectedStatus = PHASE_STATUS_MAPPING.get(target);
        if (expectedStatus == null) {
            return;
        }

        ProjectStatus currentStatus = ProjectStatus.fromCode(project.getStatus());

        // 状态已符合预期，无需变更
        if (currentStatus == expectedStatus) {
            return;
        }

        // DRAFT → IN_PROGRESS：首次进入编制阶段
        if (currentStatus == ProjectStatus.DRAFT && expectedStatus == ProjectStatus.IN_PROGRESS) {
            project.setStatus(ProjectStatus.IN_PROGRESS.getCode());
            return;
        }

        // 进入检测阶段：状态已被 onEnter 触发器设为 DETECTING，无需再转换
        if (target == ProjectPhase.DETECTION && currentStatus == ProjectStatus.DETECTING) {
            return;
        }

        // 进入检测阶段：从 IN_PROGRESS 流转
        if (target == ProjectPhase.DETECTION && currentStatus == ProjectStatus.IN_PROGRESS) {
            ProjectStateMachine.transition(project, ProjectStatus.PENDING_DETECTION);
            ProjectStateMachine.transition(project, ProjectStatus.DETECTING);
            return;
        }

        log.warn("阶段-状态不一致: phase={}, status={}, expected={}",
                target.getLabel(), currentStatus.getLabel(), expectedStatus.getLabel());
    }

    /**
     * 获取指定阶段的触发器
     */
    public PhaseTrigger getTrigger(ProjectPhase phase) {
        return triggers.get(phase);
    }
}
