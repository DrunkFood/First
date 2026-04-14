package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 招标需求来源枚举
 */
@Getter
@AllArgsConstructor
public enum RequirementSourceType {

    REFERENCE("REFERENCE", "引用"),
    SYSTEM_GENERATE("SYSTEM_GENERATE", "系统生成");

    private final String code;
    private final String label;

    public static RequirementSourceType fromCode(String code) {
        for (RequirementSourceType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的招标需求来源: " + code);
    }
}
