package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 反馈类型枚举
 */
@Getter
@AllArgsConstructor
public enum FeedbackType {

    LIKE("LIKE", "赞"),
    DISLIKE("DISLIKE", "不行");

    private final String code;
    private final String label;

    public static FeedbackType fromCode(String code) {
        for (FeedbackType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的反馈类型: " + code);
    }
}
