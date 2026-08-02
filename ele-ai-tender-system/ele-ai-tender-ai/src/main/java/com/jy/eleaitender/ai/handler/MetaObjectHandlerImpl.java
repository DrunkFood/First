package com.jy.eleaitender.ai.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.jy.eleaitender.common.constant.CommonConstant;
import com.jy.eleaitender.common.security.SecurityContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MetaObjectHandlerImpl implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        Date now = new Date();
        Long userId = SecurityContextHolder.getUserId();
        String userName = SecurityContextHolder.getUsername();
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
        String userName = SecurityContextHolder.getUsername();
        if (userId == null) { userId = 0L; }
        if (userName == null) { userName = "system"; }

        this.strictUpdateFill(metaObject, "modifyTime", Date.class, new Date());
        this.strictUpdateFill(metaObject, "modifyId", Long.class, userId);
        this.strictUpdateFill(metaObject, "modifyName", String.class, userName);
    }
}
