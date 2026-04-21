package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.core.mapper.AiProjectMapper;
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
    private AiProjectMapper aiProjectMapper;

    @Autowired
    private IRequirementService requirementService;

    @Override
    public void onEnter(AiProject project, Map<String, Object> context) {
        // 如果项目已关联需求，读取需求内容并保存
        if (project.getRequirementId() != null) {
            AiRequirement aiRequirement = requirementMapper.selectById(project.getRequirementId());
            project.setRequirementContent(aiRequirement.getContent());
            aiProjectMapper.updateById(project);
            return;
        }
        // 如果需求不存在，则创建新需求
        AiRequirement requirement = new AiRequirement();
        requirement.setRequirementName(project.getProjectName() + " - 招标需求");
        requirement.setProjectCategory(project.getProjectCategory());
        requirement.setProjectType(project.getProjectType());
        requirement.setServiceSubType(project.getServiceSubType());
        requirement.setBudget(project.getBudget());
        requirement.setStatus("IN_PROGRESS");
        requirement.setProgress(0);
        requirementService.create(requirement);
        log.info("自动创建需求记录: projectId={}, requirementId={}", project.getId(), requirement.getId());

        // 关联到项目
        project.setRequirementId(requirement.getId());
        aiProjectMapper.updateById(project);
        // 自动触发AI需求生成
        try {
            AiTask task = requirementService.submitGenerate(requirement.getId(), new HashMap<>());
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

}
