package com.jy.eleaitender.core.statemachine.trigger;

import com.jy.eleaitender.common.entity.core.TbProject;
import com.jy.eleaitender.core.statemachine.PhaseTrigger;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 基础信息阶段触发器
 * 校验项目基础信息完整性
 */
@Component
public class BasicInfoTrigger implements PhaseTrigger {

    @Override
    public boolean canComplete(TbProject project) {
        return project.getProjectName() != null && !project.getProjectName().isBlank()
                && project.getProjectCategory() != null && !project.getProjectCategory().isBlank()
                && project.getProjectType() != null && !project.getProjectType().isBlank();
    }

    @Override
    public String getIncompleteMessage() {
        return "请完善基础信息（项目名称、项目类别、项目类型为必填项）";
    }
}
