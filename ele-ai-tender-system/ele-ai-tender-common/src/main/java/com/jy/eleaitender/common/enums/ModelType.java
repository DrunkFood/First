package com.jy.eleaitender.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 模型类型枚举
 */
@Getter
@AllArgsConstructor
public enum ModelType {

    LOCAL("LOCAL", "本地微调"),
    CLOUD("CLOUD", "云端大模型"),
    PRIVATE("PRIVATE", "私有化部署");

    private final String code;
    private final String label;

    public static ModelType fromCode(String code) {
        for (ModelType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的模型类型: " + code);
    }
}
