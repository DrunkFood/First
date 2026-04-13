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
    DETECTION("DETECTION", "检测");

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
}
