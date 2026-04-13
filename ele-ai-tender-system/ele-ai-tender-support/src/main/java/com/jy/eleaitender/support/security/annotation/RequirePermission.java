package com.jy.eleaitender.support.security.annotation;

import java.lang.annotation.*;

/**
 * 权限注解
 * 用于标注需要特定权限才能访问的接口
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {

    /**
     * 需要的权限标识
     */
    String value();
}
