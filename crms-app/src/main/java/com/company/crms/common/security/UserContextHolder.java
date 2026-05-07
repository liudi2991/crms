package com.company.crms.common.security;

import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;

/**
 * 当前用户上下文持有器。在 Sa-Token 拦截器/AOP 中赋值，请求结束时清理。
 */
public final class UserContextHolder {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private UserContextHolder() {
    }

    public static void set(UserContext ctx) {
        HOLDER.set(ctx);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    public static UserContext require() {
        UserContext ctx = HOLDER.get();
        if (ctx == null) {
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return ctx;
    }

    public static Long currentUserId() {
        return require().getUserId();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
