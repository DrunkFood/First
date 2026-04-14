package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 编制阶段枚举
 */
@Getter
@AllArgsConstructor
public enum ProjectPhase {

    BASIC_INFO(1, "基础信息录入"),
    REQUIREMENT(2, "需求生成"),
    REVIEW_ITEM(3, "评审项设置"),
    DOCUMENT(4, "文档集成"),
    DETECTION(5, "智能检测");

    private final int code;
    private final String label;

    public static ProjectPhase fromCode(int code) {
        for (ProjectPhase phase : values()) {
            if (phase.code == code) {
                return phase;
            }
        }
        throw new IllegalArgumentException("未知的编制阶段: " + code);
    }

    /**
     * 获取阶段对应的进度百分比
     */
    public int getProgressPercent() {
        return this.code * 20;
    }
}
