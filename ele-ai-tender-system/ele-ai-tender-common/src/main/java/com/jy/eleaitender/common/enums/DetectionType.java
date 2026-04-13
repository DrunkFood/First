package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 检测类型枚举
 */
@Getter
@AllArgsConstructor
public enum DetectionType {

    FAIRNESS("FAIRNESS", "公平性"),
    COMPLIANCE("COMPLIANCE", "合规性"),
    TYPO("TYPO", "错别字"),
    SENSITIVE_WORD("SENSITIVE_WORD", "敏感词");

    private final String code;
    private final String label;

    public static DetectionType fromCode(String code) {
        for (DetectionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的检测类型: " + code);
    }
}
