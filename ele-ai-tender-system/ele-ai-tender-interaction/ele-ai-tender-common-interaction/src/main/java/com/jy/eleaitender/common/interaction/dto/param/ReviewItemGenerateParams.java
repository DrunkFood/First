package com.jy.eleaitender.common.interaction.dto.param;

import lombok.Data;

import java.util.List;

/**
 * 评审项生成任务参数
 */
@Data
public class ReviewItemGenerateParams implements AiTaskParams {

    private String projectName;

    private String projectType;

    private String projectCategory;

    private String budget;

    private String reviewMethod;

    private String requirementContent;

    /**
     * 评审配置(JSON字符串，可选)
     */
    private String reviewConfig;

    /**
     * 评审项配置 — 存储在模板的 review_config JSON 字段中
     */
    @Data
    public static class ReviewConfig {
        /**
         * 各评审类型配置列表
         */
        private List<ReviewTypeConfig> reviewTypes;
        /**
         * 评分模式：SCORE(分值模式,默认) / WEIGHT(权重模式)
         */
        private String scoreMode;
    }

    /**
     * 单个评审类型的配置项
     */
    @Data
    public static class ReviewTypeConfig {
        /**
         * 评审类型枚举: COMPLIANCE / TECHNICAL / CREDIT / COMMERCIAL
         */
        private String reviewType;
        /**
         * 是否启用
         */
        private boolean enabled;
        /**
         * 是否生成评审标准（false时优先使用manualItems；无手动项则item_name填充"详见评审文件"）
         */
        private boolean generateStandard;
        /**
         * 是否区分客观主观（用包装类型：null=老数据未设置，回退到 CREDIT/TECHNICAL 旧硬编码逻辑）
         */
        private Boolean distinguishSubjectivity;
    }
}
