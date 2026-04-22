package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI使用场景枚举
 */
@Getter
@AllArgsConstructor
public enum AiUsageScenario {

    GENERATION("GENERATION", "生成"),
    OPTIMIZATION("OPTIMIZATION", "优化"),
    DETECTION("DETECTION", "检测"),
    CHAT("CHAT", "对话");

    private final String code;
    private final String label;

    public static AiUsageScenario fromCode(String code) {
        for (AiUsageScenario scenario : values()) {
            if (scenario.code.equals(code)) {
                return scenario;
            }
        }
        throw new IllegalArgumentException("未知的使用场景: " + code);
    }

    /**
     * 任务类型 → 使用场景映射
     */
    public static AiUsageScenario resolveScenario(AiTaskType taskType) {
        return switch (taskType) {
            // 生成类任务
            case REQUIREMENT_GENERATE,
                 PROJECT_REQUIREMENT_GENERATE,
                 REVIEW_ITEM_GENERATE -> AiUsageScenario.GENERATION;
            // 优化类任务
            case TEXT_OPTIMIZE -> AiUsageScenario.OPTIMIZATION;
            // 检测类任务
            case DETECTION_SENSITIVE_WORD,
                 DETECTION_TYPO,
                 DETECTION_POLICY_REVIEW,
                 DETECTION_FORMAT_CHECK -> AiUsageScenario.DETECTION;
        };
    }
}
