package com.jy.eletender.support.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.jy.eletender.common.constant.CommonConstant;
import com.jy.eletender.common.security.SecurityContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * MyBatis Plus 字段自动填充处理器
 */
@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        Long userId = SecurityContextHolder.getUserId();
        String realName = SecurityContextHolder.getRealName();

        this.strictInsertFill(metaObject, "createTime", Date.class, now);
        this.strictInsertFill(metaObject, "modifyTime", Date.class, now);
        this.strictInsertFill(metaObject, "ver", Integer.class, CommonConstant.DEFAULT_VERSION);
        this.strictInsertFill(metaObject, "isDelete", Integer.class, CommonConstant.NOT_DELETED);

        this.strictInsertFill(metaObject, "createId", Long.class, userId != null ? userId : 0L);
        this.strictInsertFill(metaObject, "createName", String.class, realName != null ? realName : "system");
        this.strictInsertFill(metaObject, "modifyId", Long.class, userId != null ? userId : 0L);
        this.strictInsertFill(metaObject, "modifyName", String.class, realName != null ? realName : "system");
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = SecurityContextHolder.getUserId();
        String realName = SecurityContextHolder.getRealName();

        this.strictUpdateFill(metaObject, "modifyTime", Date.class, new Date());
        this.strictUpdateFill(metaObject, "modifyId", Long.class, userId != null ? userId : 0L);
        this.strictUpdateFill(metaObject, "modifyName", String.class, realName != null ? realName : "system");
    }
}
