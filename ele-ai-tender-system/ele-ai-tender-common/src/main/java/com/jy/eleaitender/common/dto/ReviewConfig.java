package com.jy.eleaitender.common.dto;

import com.jy.eleaitender.common.enums.ReviewType;
import com.jy.eleaitender.common.enums.ScoreMode;
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

    /**
     * 评分模式：SCORE(分值模式,默认) / WEIGHT(权重模式)
     */
    private ScoreMode scoreMode;

    private static final com.fasterxml.jackson.databind.ObjectMapper objectMapper =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * 构建默认配置：4种类型全部启用+生成评审标准
     * 区分客观主观默认仅 TECHNICAL/CREDIT 开启（与改造前硬编码现状一致）
     */
    public static ReviewConfig defaultConfig() {
        ReviewConfig config = new ReviewConfig();
        config.setReviewTypes(List.of(
                ReviewTypeConfig.of(ReviewType.COMPLIANCE.getCode(), true, true, false),
                ReviewTypeConfig.of(ReviewType.TECHNICAL.getCode(), true, true, true),
                ReviewTypeConfig.of(ReviewType.CREDIT.getCode(), true, true, true),
                ReviewTypeConfig.of(ReviewType.COMMERCIAL.getCode(), true, true, false)
        ));
        config.setScoreMode(ScoreMode.SCORE);
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
     * 判断指定评审类型是否区分客观主观
     * 字段为 null（老数据未设置）时回退到旧硬编码：CREDIT/TECHNICAL 返回 true，其他 false
     */
    public boolean isDistinguishSubjectivity(String reviewType) {
        if (reviewTypes == null) return false;
        return reviewTypes.stream()
                .filter(t -> reviewType.equals(t.getReviewType()))
                .findFirst()
                .map(t -> {
                    Boolean v = t.getDistinguishSubjectivity();
                    return v != null ? v : (ReviewType.CREDIT.getCode().equals(reviewType) || ReviewType.TECHNICAL.getCode().equals(reviewType));
                })
                .orElse(false);
    }

    /**
     * 获取所有启用的评审类型
     */
    public List<ReviewTypeConfig> getEnabledTypes() {
        if (reviewTypes == null) return List.of();
        return reviewTypes.stream().filter(ReviewTypeConfig::isEnabled).toList();
    }

    /**
     * 读取评分模式：序列化时 null 会被规范化为 SCORE，向后兼容老数据。
     * Lombok @Data 生成的 getScoreMode() 返回原始字段（可能为 null），
     * 需要非 null 语义的调用方请使用本方法或 isWeightMode()。
     */
    public ScoreMode getScoreModeOrDefault() {
        return scoreMode == null ? ScoreMode.SCORE : scoreMode;
    }

    /**
     * 仅当 scoreMode 显式为 WEIGHT 返回 true，其余（含 null 老数据）均返回 false
     */
    public boolean isWeightMode() {
        return scoreMode == ScoreMode.WEIGHT;
    }
}
