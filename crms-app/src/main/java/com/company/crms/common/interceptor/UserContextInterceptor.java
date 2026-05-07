package com.company.crms.common.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.company.crms.common.security.UserContext;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.iam.service.UserContextLoader;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 在请求开始时把当前用户上下文写入 ThreadLocal，请求结束时清理。
 */
@Component
@RequiredArgsConstructor
public class UserContextInterceptor implements HandlerInterceptor {

    private final UserContextLoader contextLoader;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (StpUtil.isLogin()) {
            Long uid = StpUtil.getLoginIdAsLong();
            UserContext ctx = contextLoader.load(uid);
            UserContextHolder.set(ctx);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContextHolder.clear();
    }
}
