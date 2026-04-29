package com.jy.eleaitender.common.dto;

import lombok.Data;

/**
 * 单个评审类型的配置项
 */
@Data
public class ReviewTypeConfig {

    /**
     * 评审类型枚举: COMPLIANCE / TECHNICAL / CREDIT / COMMERCIAL
     */
    private String reviewType;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 是否生成评审标准（false时item_name填充"详见评审文件"）
     */
    private boolean generateStandard;

    public static ReviewTypeConfig of(String reviewType, boolean enabled, boolean generateStandard) {
        ReviewTypeConfig config = new ReviewTypeConfig();
        config.setReviewType(reviewType);
        config.setEnabled(enabled);
        config.setGenerateStandard(generateStandard);
        return config;
    }
}
