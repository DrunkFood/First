package com.jy.eleaitender.common.interaction.enums;

import lombok.Getter;

/**
 * 政策文件分类枚举
 */
@Getter
public enum InteractionFileCategory {

    LAW("LAW", "法律法规"),
    REGULATION("REGULATION", "规章制度"),
    POLICY("POLICY", "政策文件");

    private final String code;
    private final String desc;

    InteractionFileCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static InteractionFileCategory getByCode(String code) {
        for (InteractionFileCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}
