package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.core.mapper.AiRequirementMapper;
import com.jy.eleaitender.core.service.IRequirementService;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 需求生成阶段触发器
 * 进入时自动创建需求并触发AI生成任务
 */
@Slf4j
@Component
public class RequirementTrigger implements PhaseTrigger {

    @Autowired
    private AiRequirementMapper requirementMapper;

    @Autowired
    private IRequirementService requirementService;

    @Override
    public void onEnter(AiProject project, Map<String, Object> context) {
        // 确保需求记录存在
        AiRequirement requirement = ensureRequirement(project);
        // 自动触发AI需求生成
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", project.getId());
        try {
            AiTask task = requirementService.submitGenerate(requirement.getId(), params);
            log.info("自动触发需求生成: projectId={}, requirementId={}, taskId={}",
                    project.getId(), requirement.getId(), task.getId());
        } catch (Exception e) {
            log.warn("自动触发需求生成失败（可能已有活跃任务）: projectId={}, error={}",
                    project.getId(), e.getMessage());
        }
    }

    @Override
    public boolean canComplete(AiProject project) {
        return project.getRequirementId() != null
                || (project.getRequirementContent() != null && !project.getRequirementContent().isBlank());
    }

    @Override
    public String getIncompleteMessage() {
        return "请先完成招标需求生成或手动填写需求内容";
    }

    /**
     * 确保项目关联的需求记录存在，不存在则自动创建
     */
    private AiRequirement ensureRequirement(AiProject project) {
        // 如果项目已关联需求，直接返回
        if (project.getRequirementId() != null) {
            return requirementMapper.selectById(project.getRequirementId());
        }

        // 查询项目下是否已有需求
        AiRequirement existing = requirementMapper.selectByProjectId(project.getId());
        if (existing != null) {
            project.setRequirementId(existing.getId());
            return existing;
        }

        // 创建新需求
        AiRequirement requirement = new AiRequirement();
        requirement.setProjectId(project.getId());
        requirement.setRequirementName(project.getProjectName() + " - 招标需求");
        requirement.setProjectCategory(project.getProjectCategory());
        requirement.setProjectType(project.getProjectType());
        requirement.setServiceSubType(project.getServiceSubType());
        requirement.setBudget(project.getBudget());
        requirement.setStatus("IN_PROGRESS");
        requirement.setProgress(0);
        requirementService.create(requirement);

        // 关联到项目
        project.setRequirementId(requirement.getId());

        log.info("自动创建需求记录: projectId={}, requirementId={}", project.getId(), requirement.getId());
        return requirement;
    }
}
