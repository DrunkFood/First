package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 匹配模式枚举
 */
@Getter
@AllArgsConstructor
public enum MatchMode {

    AUTO_MATCH("AUTO_MATCH", "自动匹配"),
    MANUAL_SELECT("MANUAL_SELECT", "手动选择"),
    UPLOAD("UPLOAD", "上传");

    private final String code;
    private final String label;

    public static MatchMode fromCode(String code) {
        for (MatchMode mode : values()) {
            if (mode.code.equals(code)) {
                return mode;
            }
        }
        return AUTO_MATCH;
    }
}
