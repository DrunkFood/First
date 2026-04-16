package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import org.springframework.stereotype.Component;

/**
 * 文档集成阶段触发器
 * 校验招标文档已生成
 */
@Component
public class DocumentTrigger implements PhaseTrigger {

    @Override
    public boolean canComplete(AiProject project) {
        return project.getGeneratedFileId() != null;
    }

    @Override
    public String getIncompleteMessage() {
        return "请先生成招标文档";
    }
}
