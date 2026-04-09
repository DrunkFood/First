package com.jy.eletender.file.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.jy.eletender.common.constant.CommonConstant;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Date;

/**
 * MyBatis Plus 配置
 */
@Configuration
public class MyBatisPlusConfig implements MetaObjectHandler {

    /**
     * 分页插件和乐观锁插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        this.strictInsertFill(metaObject, "createTime", Date.class, now);
        this.strictInsertFill(metaObject, "modifyTime", Date.class, now);
        this.strictInsertFill(metaObject, "ver", Integer.class, CommonConstant.DEFAULT_VERSION);
        this.strictInsertFill(metaObject, "isDelete", Integer.class, CommonConstant.NOT_DELETED);
        
        // 文件服务没有用户上下文，使用系统默认值
        this.strictInsertFill(metaObject, "createId", Long.class, 0L);
        this.strictInsertFill(metaObject, "createName", String.class, "system");
        this.strictInsertFill(metaObject, "modifyId", Long.class, 0L);
        this.strictInsertFill(metaObject, "modifyName", String.class, "system");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "modifyTime", Date.class, new Date());
        this.strictUpdateFill(metaObject, "modifyId", Long.class, 0L);
        this.strictUpdateFill(metaObject, "modifyName", String.class, "system");
    }
}
