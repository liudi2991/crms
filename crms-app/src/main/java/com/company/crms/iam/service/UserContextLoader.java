package com.company.crms.iam.service;

import com.company.crms.common.security.UserContext;

/**
 * 装载当前用户上下文（用户 + 角色 + 权限 + 部门集合 + 数据范围）。
 */
public interface UserContextLoader {
    /**
     * 从数据库 + Redis 中装载完整上下文。
     * 实现可加 Redis 缓存（key={@code crms:user-ctx:{userId}}），TTL 5 分钟。
     */
    UserContext load(Long userId);

    /** 强制清除缓存（如修改角色 / 权限后调用）。 */
    void invalidate(Long userId);
}
