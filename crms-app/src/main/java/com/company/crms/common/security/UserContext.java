package com.company.crms.common.security;

import com.company.crms.common.enums.DataScope;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * 当前登录用户上下文。由 Sa-Token 拦截器在请求开始时注入到 ThreadLocal。
 */
@Data
@Builder
public class UserContext {
    private Long userId;
    private String username;
    private String realName;
    private Long deptId;
    /** 包含父子关系的部门 ID 集合（DEPT 范围使用）。 */
    private Set<Long> deptIds;
    private DataScope dataScope;
    private List<String> roles;
    private Set<String> permissions;
    /** 是否为超级管理员（拥有硬删除权限）。 */
    private boolean superAdmin;

    public boolean hasPermission(String code) {
        return superAdmin || (permissions != null && permissions.contains(code));
    }
}
