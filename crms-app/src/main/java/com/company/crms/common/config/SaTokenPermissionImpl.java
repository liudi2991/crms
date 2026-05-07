package com.company.crms.common.config;

import cn.dev33.satoken.stp.StpInterface;
import com.company.crms.common.security.UserContext;
import com.company.crms.iam.service.UserContextLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限解析器：登录会话中查到当前用户的权限点与角色码。
 */
@Component
@RequiredArgsConstructor
public class SaTokenPermissionImpl implements StpInterface {

    private final UserContextLoader contextLoader;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        UserContext ctx = contextLoader.load(loginIdAsLong(loginId));
        if (ctx == null) {
            return List.of();
        }
        if (ctx.isSuperAdmin()) {
            return List.of("*");
        }
        return new ArrayList<>(ctx.getPermissions());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        UserContext ctx = contextLoader.load(loginIdAsLong(loginId));
        return ctx == null ? List.of() : ctx.getRoles();
    }

    private Long loginIdAsLong(Object loginId) {
        return loginId == null ? null : Long.valueOf(loginId.toString());
    }
}
