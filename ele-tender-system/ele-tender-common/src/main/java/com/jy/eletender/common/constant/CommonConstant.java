package com.jy.eletender.common.constant;

/**
 * 通用常量
 */
public class CommonConstant {

    private CommonConstant() {}

    /**
     * 删除标识 - 未删除
     */
    public static final int NOT_DELETED = 0;

    /**
     * 删除标识 - 已删除
     */
    public static final int DELETED = 1;

    /**
     * 默认版本号
     */
    public static final int DEFAULT_VERSION = 1;

    /**
     * Token类型 - 内部用户
     */
    public static final String TOKEN_TYPE_INTERNAL = "INTERNAL";

    /**
     * Token类型 - 外部系统
     */
    public static final String TOKEN_TYPE_EXTERNAL = "EXTERNAL";

    /**
     * 默认管理员角色编码
     */
    public static final String ADMIN_ROLE_CODE = "ADMIN";

    /**
     * 默认分页大小
     */
    public static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 最大分页大小
     */
    public static final int MAX_PAGE_SIZE = 100;
}
