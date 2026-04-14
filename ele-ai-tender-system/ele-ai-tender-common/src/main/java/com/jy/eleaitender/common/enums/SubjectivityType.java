package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 客观/主观枚举（技术标/资信标评审专用）
 */
@Getter
@AllArgsConstructor
public enum SubjectivityType {

    OBJECTIVE("OBJECTIVE", "客观"),
    SUBJECTIVE("SUBJECTIVE", "主观");

    private final String code;
    private final String label;

    public static SubjectivityType fromCode(String code) {
        for (SubjectivityType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的主客观类型: " + code);
    }
}
