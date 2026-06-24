package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.common.entity.core.TbRequirement;
import com.jy.eleaitender.common.entity.core.TbProjectReviewItem;
import com.jy.eleaitender.core.mapper.TbRequirementMapper;
import com.jy.eleaitender.core.mapper.TbProjectReviewItemMapper;
import com.jy.eleaitender.core.service.IReviewItemService;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评审项设置阶段触发器
 * 进入时自动触发AI评审项生成
 */
@Slf4j
@Component
public class ReviewItemTrigger implements PhaseTrigger {

    @Autowired
    private TbProjectReviewItemMapper reviewItemMapper;

    @Autowired
    private IReviewItemService reviewItemService;

    @Autowired
    private TbRequirementMapper requirementMapper;

    @Override
    public void onEnter(TbProject project, Map<String, Object> context) {
        // 构建包含项目信息的参数
        Map<String, Object> params = new HashMap<>();
        params.put("projectId", project.getId());
        params.put("projectName", project.getProjectName());
        params.put("projectType", project.getProjectType());
        params.put("projectCategory", project.getProjectCategory());
        params.put("budget", project.getBudget() != null ? project.getBudget().toPlainString() : null);
        params.put("reviewMethod", project.getReviewType());
        params.put("requirementContent", project.getRequirementContent());

        try {
            AiTask task = reviewItemService.submitGenerate(project.getId(), params);
            log.info("自动触发评审项生成: projectId={}, taskId={}", project.getId(), task.getId());
        } catch (Exception e) {
            log.warn("自动触发评审项生成失败（可能已有活跃任务）: projectId={}, error={}",
                    project.getId(), e.getMessage(), e);
        }
    }

    @Override
    public boolean canComplete(TbProject project) {
        List<TbProjectReviewItem> items = reviewItemMapper.selectByProjectId(project.getId());
        return items != null && !items.isEmpty();
    }

    @Override
    public String getIncompleteMessage() {
        return "请先设置评审项";
    }
}
