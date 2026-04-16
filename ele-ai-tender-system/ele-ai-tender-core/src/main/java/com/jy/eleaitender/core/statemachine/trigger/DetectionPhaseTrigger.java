package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.core.AiProject;
import com.jy.eleaitender.common.enums.ProjectStatus;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import org.springframework.stereotype.Component;

/**
 * 智能检测阶段触发器
 * 进入时自动触发检测，完成校验依赖检测结果
 */
@Component
public class DetectionPhaseTrigger implements PhaseTrigger {

    @Override
    public boolean canComplete(AiProject project) {
        String status = project.getStatus();
        return ProjectStatus.DETECTION_PASSED.getCode().equals(status)
                || ProjectStatus.DETECTION_SKIPPED.getCode().equals(status);
    }

    @Override
    public String getIncompleteMessage() {
        return "请等待智能检测完成或跳过检测";
    }
}
