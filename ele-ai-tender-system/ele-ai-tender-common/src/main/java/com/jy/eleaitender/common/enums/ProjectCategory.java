package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目类别枚举
 */
@Getter
@AllArgsConstructor
public enum ProjectCategory {

    LIMITED_BELOW("LIMITED_BELOW", "限额以下"),
    PROPERTY_TRADE("PROPERTY_TRADE", "产权交易"),
    GOVERNMENT_PROCUREMENT("GOVERNMENT_PROCUREMENT", "政府采购");

    private final String code;
    private final String label;

    public static ProjectCategory fromCode(String code) {
        for (ProjectCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("未知的项目类别: " + code);
    }
}
