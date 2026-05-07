package com.company.crms.iam.service.impl;

import com.company.crms.common.enums.DataScope;
import com.company.crms.common.security.UserContext;
import com.company.crms.iam.entity.User;
import com.company.crms.iam.mapper.DepartmentMapper;
import com.company.crms.iam.mapper.UserMapper;
import com.company.crms.iam.service.UserContextLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserContextLoaderImpl implements UserContextLoader {

    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final StringRedisTemplate redis;

    private static final String CACHE_KEY = "crms:user-ctx:";

    @Override
    public UserContext load(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);
        if (roles == null) {
            roles = List.of();
        }
        List<String> perms = userMapper.selectPermissionsByUserId(userId);
        if (perms == null) {
            perms = List.of();
        }
        String scopeStr = userMapper.selectMaxDataScope(userId);
        DataScope scope = DataScope.of(scopeStr);
        boolean superAdmin = user.getSuperAdmin() != null && user.getSuperAdmin() == 1;
        Set<Long> deptIds;
        if (scope == DataScope.DEPT) {
            List<Long> tree = departmentMapper.selectSelfAndDescendantIds(user.getDeptId());
            deptIds = new HashSet<>(tree != null ? tree : List.of());
        } else if (user.getDeptId() != null) {
            deptIds = Set.of(user.getDeptId());
        } else {
            deptIds = Set.of();
        }

        return UserContext.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .deptId(user.getDeptId())
                .deptIds(deptIds)
                .dataScope(superAdmin ? DataScope.ALL : scope)
                .roles(roles)
                .permissions(new HashSet<>(perms))
                .superAdmin(superAdmin)
                .build();
    }

    @Override
    public void invalidate(Long userId) {
        try {
            redis.delete(CACHE_KEY + userId);
        } catch (Exception e) {
            log.warn("invalidate user context cache failed: {}", e.getMessage());
        }
    }
}
