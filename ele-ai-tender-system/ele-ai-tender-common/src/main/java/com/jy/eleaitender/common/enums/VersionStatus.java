package com.jy.eleaitender.common.enums;

/**
 * 版本状态枚举
 */
public enum VersionStatus {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    OFFLINE(2, "已下线");

    private final int code;
    private final String desc;

    VersionStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static VersionStatus getByCode(int code) {
        for (VersionStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
