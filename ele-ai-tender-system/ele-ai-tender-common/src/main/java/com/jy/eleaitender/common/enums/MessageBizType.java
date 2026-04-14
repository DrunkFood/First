package com.jy.eleaitender.common.enums;

/**
 * 消息关联业务类型枚举
 */
public enum MessageBizType {

    PROJECT("PROJECT", "项目"),
    REQUIREMENT("REQUIREMENT", "需求"),
    DETECTION("DETECTION", "检测"),
    TEMPLATE("TEMPLATE", "模板");

    private final String code;
    private final String desc;

    MessageBizType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static MessageBizType getByCode(String code) {
        for (MessageBizType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
