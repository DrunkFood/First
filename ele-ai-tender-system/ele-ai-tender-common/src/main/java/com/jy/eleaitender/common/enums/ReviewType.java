package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 评审类型枚举
 */
@Getter
@AllArgsConstructor
public enum ReviewType {

    COMPLIANCE("COMPLIANCE", "符合性审查"),
    TECHNICAL("TECHNICAL", "技术标评审"),
    CREDIT("CREDIT", "资信标评审"),
    COMMERCIAL("COMMERCIAL", "商务评审");

    private final String code;
    private final String label;

    public static ReviewType fromCode(String code) {
        for (ReviewType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的评审类型: " + code);
    }

    public static String getLabels() {
        return Arrays.stream(ReviewType.values()).map(ReviewType::getLabel).collect(Collectors.joining("、"));
    }
}
