package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 评审方式
 */
@Getter
@AllArgsConstructor
public enum ReviewMethod {

    INTELLIGENT("INTELLIGENT", "智能评标"),
    MANUAL("MANUAL", "人工评审");

    private final String code;
    private final String label;

    public static ReviewMethod fromCode(String code) {
        for (ReviewMethod type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的评审方式: " + code);
    }

    public static String getLabel(String code) {
        for (ReviewMethod type : values()) {
            if (type.code.equals(code)) {
                return type.label;
            }
        }
        return code;
    }
}
