package com.jy.eleaitender.common.enums;

/**
 * 消息类型枚举
 */
public enum MessageType {

    SYSTEM("SYSTEM", "系统通知"),
    AUDIT("AUDIT", "审核通知"),
    DETECTION("DETECTION", "检测通知"),
    WARNING("WARNING", "预警通知");

    private final String code;
    private final String desc;

    MessageType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static MessageType getByCode(String code) {
        for (MessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
