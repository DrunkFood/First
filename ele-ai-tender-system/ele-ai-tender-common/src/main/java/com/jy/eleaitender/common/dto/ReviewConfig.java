package com.jy.eleaitender.common.dto;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 评审项配置 — 存储在模板的 review_config JSON 字段中
 */
@Data
public class ReviewConfig {

    private static final Logger log = LoggerFactory.getLogger(ReviewConfig.class);

    /**
     * 各评审类型配置列表
     */
    private List<ReviewTypeConfig> reviewTypes;

    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 构建默认配置：4种类型全部启用+生成评审标准
     */
    public static ReviewConfig defaultConfig() {
        ReviewConfig config = new ReviewConfig();
        config.setReviewTypes(List.of(
                ReviewTypeConfig.of("COMPLIANCE", true, true),
                ReviewTypeConfig.of("TECHNICAL", true, true),
                ReviewTypeConfig.of("CREDIT", true, true),
                ReviewTypeConfig.of("COMMERCIAL", true, true)
        ));
        return config;
    }

    /**
     * 从JSON字符串解析
     */
    public static ReviewConfig fromJson(String json) {
        try {
            return objectMapper.readValue(json, ReviewConfig.class);
        } catch (Exception e) {
            log.warn("ReviewConfig JSON解析失败，回退默认配置: {}", e.getMessage(), e);
            return defaultConfig();
        }
    }

    /**
     * 判断指定评审类型是否启用
     */
    public boolean isEnabled(String reviewType) {
        return reviewTypes != null && reviewTypes.stream()
                .anyMatch(t -> reviewType.equals(t.getReviewType()) && t.isEnabled());
    }

    /**
     * 判断指定评审类型是否需要生成评审标准
     */
    public boolean isGenerateStandard(String reviewType) {
        return reviewTypes != null && reviewTypes.stream()
                .anyMatch(t -> reviewType.equals(t.getReviewType()) && t.isGenerateStandard());
    }

    /**
     * 获取所有启用的评审类型
     */
    public List<ReviewTypeConfig> getEnabledTypes() {
        if (reviewTypes == null) return List.of();
        return reviewTypes.stream().filter(ReviewTypeConfig::isEnabled).toList();
    }
}
