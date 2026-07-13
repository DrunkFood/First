package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI任务类型枚举
 */
@Getter
@AllArgsConstructor
public enum AiTaskSource {
    INTERNAL("INTERNAL", "内部任务"),

    EXTERNAL("EXTERNAL", "外部任务"),
    ;

    private final String code;
    private final String label;

    public static AiTaskSource fromCode(String code) {
        for (AiTaskSource type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的AI任务类型: " + code);
    }

}
