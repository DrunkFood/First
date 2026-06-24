package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbRequirement;
import com.jy.eleaitender.common.enums.AiTaskType;
import com.jy.eleaitender.core.mapper.TbProjectMapper;
import com.jy.eleaitender.core.mapper.TbRequirementMapper;
import com.jy.eleaitender.core.service.IAiTaskService;
import com.jy.eleaitender.core.service.IProjectService;
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
    private TbRequirementMapper requirementMapper;

    @Autowired
    private TbProjectMapper tbProjectMapper;

    @Autowired
    private IProjectService projectService;

    @Override
    public void onEnter(TbProject project, Map<String, Object> context) {
        // 如果项目已关联需求，读取需求内容并保存
        if (project.getRequirementId() != null) {
            TbRequirement aiRequirement = requirementMapper.selectById(project.getRequirementId());
            project.setRequirementContent(aiRequirement.getContent());
            tbProjectMapper.updateById(project);
            return;
        }
        // 如果需求不存在，自动触发AI需求生成
        try {
            AiTask task = projectService.generateRequirement(project.getId());
            log.info("自动触发项目需求生成: projectId={}, taskId={}",
                    project.getId(), task.getId());
        } catch (Exception e) {
            log.warn("自动触发项目需求生成失败（可能已有活跃任务）: projectId={}, error={}",
                    project.getId(), e.getMessage(), e);
        }
    }

    @Override
    public boolean canComplete(TbProject project) {
        return project.getRequirementId() != null
                || (project.getRequirementContent() != null && !project.getRequirementContent().isBlank());
    }

    @Override
    public String getIncompleteMessage() {
        return "请先完成招标需求生成或手动填写需求内容";
    }

}
