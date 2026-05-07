package com.company.crms.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.company.crms.common.security.UserContext;
import com.company.crms.common.security.UserContextHolder;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充：审计字段 + 软删除标志 + 乐观锁版本。
 */
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long uid = currentUserId();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "createdBy", Long.class, uid);
        strictInsertFill(metaObject, "updatedBy", Long.class, uid);
        strictInsertFill(metaObject, "isDeleted", Integer.class, 0);
        strictInsertFill(metaObject, "version", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId());
    }

    private Long currentUserId() {
        UserContext ctx = UserContextHolder.get();
        return ctx == null ? 0L : ctx.getUserId();
    }
}
