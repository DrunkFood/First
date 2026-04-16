package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import org.springframework.stereotype.Component;

/**
 * 需求生成阶段触发器
 * 校验招标需求已生成或手动填写
 */
@Component
public class RequirementTrigger implements PhaseTrigger {

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
