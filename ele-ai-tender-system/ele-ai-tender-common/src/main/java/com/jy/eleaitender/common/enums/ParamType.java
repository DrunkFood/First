package com.jy.eleaitender.common.enums;

/**
 * 系统参数值类型枚举
 */
public enum ParamType {

    STRING("STRING", "字符串"),
    NUMBER("NUMBER", "数字"),
    BOOLEAN("BOOLEAN", "布尔值"),
    JSON("JSON", "JSON文本");

    private final String code;
    private final String desc;

    ParamType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ParamType getByCode(String code) {
        for (ParamType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
