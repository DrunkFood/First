package com.jy.eleaitender.common.logging;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于标注需要记录操作日志的接口方法
 * <p>
 * 使用示例: @OperationLog("创建用户")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作描述
     */
    String value();
}
