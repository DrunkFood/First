package com.jy.eleaitender.common.interaction.dto.param;

import lombok.Data;

/**
 * 需求生成任务参数
 */
@Data
public class RequirementGenerateParams implements AiTaskParams {

    private String requirementName;

    private String projectType;

    private String projectCategory;

    private String budget;

    private String description;

}
