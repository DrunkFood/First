package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.entity.core.AiRequirement;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.core.mapper.AiProjectMapper;
import com.jy.eleaitender.core.mapper.AiRequirementMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IRequirementService;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
    private IAiTaskService aiTaskService;

    @Override
    public void onEnter(AiProject project, Map<String, Object> context) {
        // 如果项目已关联需求，读取需求内容并保存
        if (project.getRequirementId() != null) {
            AiRequirement aiRequirement = requirementMapper.selectById(project.getRequirementId());
            project.setRequirementContent(aiRequirement.getContent());
            aiProjectMapper.updateById(project);
            return;
        }
        // 如果需求不存在，自动触发AI需求生成
        try {
            // 填充任务参数
            Map<String, Object> params = new HashMap<>();
            params.put("requirementName", project.getProjectName() + " - 招标需求");
            params.put("projectType", project.getProjectType());
            params.put("projectCategory", project.getProjectCategory());
            params.put("budget", project.getBudget());
            params.put("description", project.getProjectDescription());
            // TODO 参考文档内容 从 自动匹配的第一份文件/手动选择匹配的历史文件id/上传的文件id 中获取
            params.put("referenceContent", "");
            AiTask task = aiTaskService.createTask(AiTaskType.PROJECT_REQUIREMENT_GENERATE, null,
                    project.getId(), "REQUIREMENT", params, null);
            log.info("自动触发需求生成: projectId={}, requirementId={}, taskId={}",
                    project.getId(), project.getId(), task.getId());
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
