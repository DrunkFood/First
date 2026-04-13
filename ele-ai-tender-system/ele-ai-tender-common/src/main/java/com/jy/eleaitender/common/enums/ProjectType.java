package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目类型枚举
 */
@Getter
@AllArgsConstructor
public enum ProjectType {

    ENGINEERING("ENGINEERING", "工程"),
    GOODS("GOODS", "货物"),
    SERVICE("SERVICE", "服务");

    private final String code;
    private final String label;

    public static ProjectType fromCode(String code) {
        for (ProjectType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的项目类型: " + code);
    }
}
