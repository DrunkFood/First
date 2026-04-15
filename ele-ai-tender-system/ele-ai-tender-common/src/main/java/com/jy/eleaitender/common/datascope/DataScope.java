package com.jy.eleaitender.common.datascope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据隔离注解
 * 标记在 Mapper 方法或类上，用于控制是否跳过数据隔离过滤
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {

    /**
     * 是否跳过数据隔离
     * 用于管理后台统计、跨用户匹配等需要查看全量数据的场景
     */
    boolean skip() default false;
}
