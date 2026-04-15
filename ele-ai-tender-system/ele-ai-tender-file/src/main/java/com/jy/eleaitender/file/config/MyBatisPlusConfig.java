package com.jy.eleaitender.file.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.jy.eleaitender.common.constant.CommonConstant;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * MyBatis Plus 配置
 */
@Configuration
public class MyBatisPlusConfig implements MetaObjectHandler {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getRealName();
        if (userId == null) { userId = 0L; }
        if (userName == null) { userName = "system"; }

        this.strictInsertFill(metaObject, "createTime", Date.class, now);
        this.strictInsertFill(metaObject, "modifyTime", Date.class, now);
        this.strictInsertFill(metaObject, "ver", Integer.class, CommonConstant.DEFAULT_VERSION);
        this.strictInsertFill(metaObject, "isDelete", Integer.class, CommonConstant.NOT_DELETED);
        this.strictInsertFill(metaObject, "createId", Long.class, userId);
        this.strictInsertFill(metaObject, "createName", String.class, userName);
        this.strictInsertFill(metaObject, "modifyId", Long.class, userId);
        this.strictInsertFill(metaObject, "modifyName", String.class, userName);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getRealName();
        if (userId == null) { userId = 0L; }
        if (userName == null) { userName = "system"; }

        this.strictUpdateFill(metaObject, "modifyTime", Date.class, new Date());
        this.strictUpdateFill(metaObject, "modifyId", Long.class, userId);
        this.strictUpdateFill(metaObject, "modifyName", String.class, userName);
    }
}
