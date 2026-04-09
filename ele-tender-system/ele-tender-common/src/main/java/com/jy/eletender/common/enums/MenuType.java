package com.jy.eletender.common.enums;

/**
 * 菜单类型枚举
 */
public enum MenuType {

    MENU(1, "菜单"),
    BUTTON(2, "按钮");

    private final int code;
    private final String desc;

    MenuType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static MenuType getByCode(int code) {
        for (MenuType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
