package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.core.AiProject;
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
    public void onEnter(AiProject project, Map<String, Object> context) {
        // 自动执行文档集成
        try {
            documentIntegrationService.integrate(project.getId());
            log.info("自动执行文档集成: projectId={}", project.getId());
        } catch (Exception e) {
            log.warn("自动文档集成失败: projectId={}, error={}", project.getId(), e.getMessage());
        }
    }

    @Override
    public boolean canComplete(AiProject project) {
        return project.getGeneratedFileId() != null;
    }

    @Override
    public String getIncompleteMessage() {
        return "请先生成招标文档";
    }
}
