package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 需求来源枚举
 */
@Getter
@AllArgsConstructor
public enum RequirementSource {

    REFERENCE("REFERENCE", "参考历史"),
    AI_GENERATED("AI_GENERATED", "AI生成");

    private final String code;
    private final String label;

    public static RequirementSource fromCode(String code) {
        for (RequirementSource source : values()) {
            if (source.code.equals(code)) {
                return source;
            }
        }
        throw new IllegalArgumentException("未知的需求来源: " + code);
    }
}
