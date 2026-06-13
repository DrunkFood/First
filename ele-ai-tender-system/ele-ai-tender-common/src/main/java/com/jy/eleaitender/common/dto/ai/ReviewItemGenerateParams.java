package com.jy.eleaitender.common.dto.ai;

import lombok.Data;

/**
 * 评审项生成任务参数
 */
@Data
public class ReviewItemGenerateParams {

    private String projectName;

    private String projectType;

    private String projectCategory;

    private String budget;

    private String reviewMethod;

    private String requirementContent;

    /** 评审配置(JSON字符串，可选) */
    private String reviewConfig;
}
