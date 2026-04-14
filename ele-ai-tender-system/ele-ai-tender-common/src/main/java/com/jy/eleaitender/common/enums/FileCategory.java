package com.jy.eleaitender.common.enums;

/**
 * 政策文件分类枚举
 */
public enum FileCategory {

    LAW("LAW", "法律法规"),
    REGULATION("REGULATION", "规章制度"),
    POLICY("POLICY", "政策文件");

    private final String code;
    private final String desc;

    FileCategory(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static FileCategory getByCode(String code) {
        for (FileCategory category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        return null;
    }
}
