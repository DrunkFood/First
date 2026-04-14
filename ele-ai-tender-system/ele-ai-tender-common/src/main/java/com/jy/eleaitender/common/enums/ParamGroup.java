package com.jy.eleaitender.common.enums;

/**
 * 系统参数分组枚举
 */
public enum ParamGroup {

    SYSTEM("SYSTEM", "系统参数"),
    SWITCH("SWITCH", "功能开关"),
    AI_RULE("AI_RULE", "AI检测规则");

    private final String code;
    private final String desc;

    ParamGroup(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ParamGroup getByCode(String code) {
        for (ParamGroup group : values()) {
            if (group.code.equals(code)) {
                return group;
            }
        }
        return null;
    }
}
