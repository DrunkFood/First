package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务类型枚举
 */
@Getter
@AllArgsConstructor
public enum BizType {

    REQUIREMENT("REQUIREMENT", "需求"),
    PROJECT("PROJECT", "项目"),
    DOCUMENT("DOCUMENT", "文档"),
    REVIEW_ITEM("REVIEW_ITEM", "评审项"),
    DETECTION("DETECTION", "检测"),
    ;

    private final String code;
    private final String label;

    public static BizType fromCode(String code) {
        for (BizType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的业务类型: " + code);
    }

}
