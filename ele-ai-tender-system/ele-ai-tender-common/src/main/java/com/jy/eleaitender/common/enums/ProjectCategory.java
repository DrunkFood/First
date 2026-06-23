package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目类别枚举
 */
@Getter
@AllArgsConstructor
public enum ProjectCategory {

    SMALL_TRADE("SMALL_TRADE", "小额交易"),
    GOVERNMENT_PROCUREMENT("GOVERNMENT_PROCUREMENT", "政府采购"),
    COMPREHENSIVE_TRADE("COMPREHENSIVE_TRADE", "综合交易");

    private final String code;
    private final String label;

    public static ProjectCategory fromCode(String code) {
        for (ProjectCategory type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的项目类别: " + code);
    }
}
