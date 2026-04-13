package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目状态枚举
 */
@Getter
@AllArgsConstructor
public enum ProjectStatus {

    DRAFT("DRAFT", "草稿"),
    IN_PROGRESS("IN_PROGRESS", "编制中"),
    PENDING_DETECTION("PENDING_DETECTION", "待检测"),
    DETECTING("DETECTING", "检测中"),
    DETECTION_PASSED("DETECTION_PASSED", "检测通过"),
    DETECTION_FAILED("DETECTION_FAILED", "检测未通过"),
    DETECTION_SKIPPED("DETECTION_SKIPPED", "已跳过检测"),
    PUBLISHED("PUBLISHED", "已发布"),
    ARCHIVED("ARCHIVED", "已归档"),
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String label;

    public static ProjectStatus fromCode(String code) {
        for (ProjectStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的项目状态: " + code);
    }
}
