package com.jy.eletender.common.constant;

/**
 * Redis Key 常量
 */
public class RedisKeyConstant {

    private RedisKeyConstant() {}

    /**
     * 内部用户Token前缀
     */
    public static final String TOKEN_PREFIX = "token:";

    /**
     * 外部用户Token前缀
     */
    public static final String EXTERNAL_TOKEN_PREFIX = "external:token:";

    /**
     * 用户权限缓存前缀
     */
    public static final String USER_PERMISSIONS_PREFIX = "user:permissions:";

    /**
     * 用户菜单缓存前缀
     */
    public static final String USER_MENUS_PREFIX = "user:menus:";

    /**
     * 角色权限缓存前缀
     */
    public static final String ROLE_PERMISSIONS_PREFIX = "role:permissions:";

    /**
     * 文件上传锁前缀
     */
    public static final String FILE_UPLOAD_LOCK_PREFIX = "file:upload:lock:";

    /**
     * 默认Token过期时间（秒）- 2小时
     */
    public static final long TOKEN_EXPIRE_SECONDS = 2 * 60 * 60;

    /**
     * 外部Token过期时间（秒）- 7天
     */
    public static final long EXTERNAL_TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    /**
     * 权限缓存过期时间（秒）- 30分钟
     */
    public static final long PERMISSION_CACHE_EXPIRE_SECONDS = 30 * 60;

    /**
     * 已注册的自定义文件后缀集合（Set 类型）
     */
    public static final String REGISTERED_SUFFIXES = "file:registered-suffixes";
}
