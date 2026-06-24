package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.ai.AiTask;
import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.core.service.IDocumentIntegrationService;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 文档集成阶段触发器
 * 进入时自动执行文档集成
 */
@Slf4j
@Component
public class DocumentTrigger implements PhaseTrigger {

    @Autowired
    private IDocumentIntegrationService documentIntegrationService;

    @Override
    public void onEnter(TbProject project, Map<String, Object> context) {
        // 自动执行文档集成
        try {
            AiTask task = documentIntegrationService.integrate(project.getId());
            log.info("自动触发项目文档集成: projectId={}, taskId={}",
                    project.getId(), task.getId());
        } catch (Exception e) {
            log.warn("自动触发项目文档集成失败（可能已有活跃任务）: projectId={}, error={}",
                    project.getId(), e.getMessage(), e);
        }
    }

    @Override
    public boolean canComplete(TbProject project) {
        return project.getGeneratedFileId() != null;
    }

    @Override
    public String getIncompleteMessage() {
        return "请先生成招标文档";
    }
}
