package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 检测类型枚举
 */
@Getter
@AllArgsConstructor
public enum DetectionType {

    SENSITIVE_WORD("SENSITIVE_WORD", "敏感词检测"),
    TYPO("TYPO", "错别字检测"),
    POLICY_REVIEW("POLICY_REVIEW", "政策文件审查"),
    FORMAT_CHECK("FORMAT_CHECK", "格式规范检测");

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
