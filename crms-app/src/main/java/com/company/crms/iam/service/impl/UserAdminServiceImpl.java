package com.company.crms.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.iam.dto.AssignRolesDTO;
import com.company.crms.iam.dto.CreateUserDTO;
import com.company.crms.iam.dto.ResetPasswordDTO;
import com.company.crms.iam.dto.UpdateUserDTO;
import com.company.crms.iam.dto.UserQuery;
import com.company.crms.iam.entity.Department;
import com.company.crms.iam.entity.Role;
import com.company.crms.iam.entity.User;
import com.company.crms.iam.mapper.DepartmentMapper;
import com.company.crms.iam.mapper.RoleMapper;
import com.company.crms.iam.mapper.UserMapper;
import com.company.crms.iam.mapper.UserRoleMapper;
import com.company.crms.iam.service.UserAdminService;
import com.company.crms.iam.service.UserContextLoader;
import com.company.crms.iam.vo.UserVO;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAdminServiceImpl implements UserAdminService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final DepartmentMapper departmentMapper;
    private final UserRoleMapper userRoleMapper;
    private final UserContextLoader contextLoader;

    @Value("${crms.security.default-password:Crms@123456}")
    private String defaultPassword;

    @Override
    public PageResult<UserVO> page(UserQuery query) {
        Page<User> page = Page.of(query.getPage(), query.getSize());
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            String kw = query.getKeyword().trim();
            wrapper.and(w -> w.like("username", kw)
                    .or().like("real_name", kw)
                    .or().like("phone", kw));
        }
        if (query.getDeptId() != null) {
            List<Long> deptIds = departmentMapper.selectSelfAndDescendantIds(query.getDeptId());
            if (deptIds == null || deptIds.isEmpty()) {
                deptIds = List.of(query.getDeptId());
            }
            wrapper.in("dept_id", deptIds);
        }
        if (query.getStatus() != null && !query.getStatus().isBlank()) {
            wrapper.eq("status", query.getStatus());
        }
        wrapper.orderByDesc("created_at");

        Page<User> result = userMapper.selectPage(page, wrapper);
        List<UserVO> vos = result.getRecords().stream().map(this::toVO).toList();
        // 批量补 dept name + roles
        enrichDeptAndRoles(vos);
        return PageResult.of(result, vos);
    }

    @Override
    public UserVO detail(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        UserVO vo = toVO(u);
        enrichDeptAndRoles(List.of(vo));
        return vo;
    }

    @Override
    @OperationLog(module = "用户", action = "新建用户", type = "CREATE")
    @Transactional
    public Long create(CreateUserDTO dto) {
        Department dept = departmentMapper.selectById(dto.getDeptId());
        if (dept == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        // 校验角色全部存在
        List<Role> roles = roleMapper.selectBatchIds(dto.getRoleIds());
        if (roles.size() != dto.getRoleIds().size()) {
            throw new BizException(ErrorCode.NOT_FOUND, "存在不存在的角色");
        }

        User u = new User();
        u.setId(SnowflakeIdGenerator.next());
        u.setUsername(dto.getUsername());
        u.setRealName(dto.getRealName());
        u.setPhone(dto.getPhone());
        u.setEmail(dto.getEmail());
        u.setDeptId(dto.getDeptId());
        u.setStatus("ACTIVE");
        u.setSuperAdmin(0);
        String pwd = (dto.getPassword() == null || dto.getPassword().isBlank()) ? defaultPassword : dto.getPassword();
        u.setPasswordHash(BCrypt.hashpw(pwd, BCrypt.gensalt(10)));
        u.setMustChangePwd(1);
        u.setFailedCount(0);
        u.setDataScope("SELF");
        try {
            userMapper.insert(u);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "用户名已存在");
        }
        replaceRoles(u.getId(), dto.getRoleIds());
        return u.getId();
    }

    @Override
    @OperationLog(module = "用户", action = "更新用户", type = "UPDATE")
    @Transactional
    public void update(Long id, UpdateUserDTO dto) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        Department dept = departmentMapper.selectById(dto.getDeptId());
        if (dept == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        User upd = new User();
        upd.setId(id);
        upd.setRealName(dto.getRealName());
        upd.setPhone(dto.getPhone());
        upd.setEmail(dto.getEmail());
        upd.setDeptId(dto.getDeptId());
        userMapper.updateById(upd);
        contextLoader.invalidate(id);
    }

    @Override
    @OperationLog(module = "用户", action = "删除用户", type = "DELETE")
    @Transactional
    public void softDelete(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (u.getSuperAdmin() != null && u.getSuperAdmin() == 1) {
            throw new BizException(ErrorCode.AUTH_FORBIDDEN, "不可删除超级管理员");
        }
        if (Objects.equals(id, UserContextHolder.currentUserId())) {
            throw new BizException(ErrorCode.AUTH_FORBIDDEN, "不可删除自己");
        }
        userMapper.deleteById(id);
        userRoleMapper.deleteByUserId(id);
        contextLoader.invalidate(id);
    }

    @Override
    @OperationLog(module = "用户", action = "重置密码", type = "UPDATE", recordParams = false)
    @Transactional
    public void resetPassword(Long id, ResetPasswordDTO dto) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        String pwd = (dto.getNewPassword() == null || dto.getNewPassword().isBlank())
                ? defaultPassword : dto.getNewPassword();
        User upd = new User();
        upd.setId(id);
        upd.setPasswordHash(BCrypt.hashpw(pwd, BCrypt.gensalt(10)));
        upd.setMustChangePwd(1);
        upd.setFailedCount(0);
        upd.setLockedUntil(null);
        userMapper.updateById(upd);
        contextLoader.invalidate(id);
    }

    @Override
    @OperationLog(module = "用户", action = "分配角色", type = "UPDATE")
    @Transactional
    public void assignRoles(Long id, AssignRolesDTO dto) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (u.getSuperAdmin() != null && u.getSuperAdmin() == 1) {
            throw new BizException(ErrorCode.AUTH_FORBIDDEN, "不可修改超级管理员的角色");
        }
        List<Role> roles = roleMapper.selectBatchIds(dto.getRoleIds());
        if (roles.size() != dto.getRoleIds().size()) {
            throw new BizException(ErrorCode.NOT_FOUND, "存在不存在的角色");
        }
        replaceRoles(id, dto.getRoleIds());
        contextLoader.invalidate(id);
    }

    @Override
    @OperationLog(module = "用户", action = "停用用户", type = "UPDATE")
    public void disable(Long id) {
        ensureNotSuperOrSelf(id, "停用");
        User upd = new User();
        upd.setId(id);
        upd.setStatus("DISABLED");
        userMapper.updateById(upd);
        contextLoader.invalidate(id);
    }

    @Override
    @OperationLog(module = "用户", action = "启用用户", type = "UPDATE")
    public void enable(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        User upd = new User();
        upd.setId(id);
        upd.setStatus("ACTIVE");
        userMapper.updateById(upd);
        contextLoader.invalidate(id);
    }

    @Override
    @OperationLog(module = "用户", action = "解锁用户", type = "UPDATE")
    public void unlock(Long id) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        User upd = new User();
        upd.setId(id);
        upd.setFailedCount(0);
        upd.setLockedUntil(null);
        userMapper.updateById(upd);
        contextLoader.invalidate(id);
    }

    private void ensureNotSuperOrSelf(Long id, String action) {
        User u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (u.getSuperAdmin() != null && u.getSuperAdmin() == 1) {
            throw new BizException(ErrorCode.AUTH_FORBIDDEN, "不可" + action + "超级管理员");
        }
        if (Objects.equals(id, UserContextHolder.currentUserId())) {
            throw new BizException(ErrorCode.AUTH_FORBIDDEN, "不可" + action + "自己");
        }
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.deleteByUserId(userId);
        for (Long rid : roleIds) {
            userRoleMapper.insert(SnowflakeIdGenerator.next(), userId, rid);
        }
    }

    private UserVO toVO(User u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setRealName(u.getRealName());
        vo.setPhone(u.getPhone());
        vo.setEmail(u.getEmail());
        vo.setDeptId(u.getDeptId());
        vo.setStatus(u.getStatus());
        vo.setSuperAdmin(u.getSuperAdmin() != null && u.getSuperAdmin() == 1);
        vo.setMustChangePwd(u.getMustChangePwd() != null && u.getMustChangePwd() == 1);
        vo.setLocked(u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now()));
        vo.setLastLoginAt(u.getLastLoginAt());
        vo.setCreatedAt(u.getCreatedAt());
        vo.setVersion(u.getVersion());
        return vo;
    }

    private void enrichDeptAndRoles(List<UserVO> vos) {
        if (vos == null || vos.isEmpty()) {
            return;
        }
        Set<Long> deptIds = vos.stream().map(UserVO::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptNames = new HashMap<>();
        if (!deptIds.isEmpty()) {
            for (Department d : departmentMapper.selectBatchIds(deptIds)) {
                deptNames.put(d.getId(), d.getName());
            }
        }
        for (UserVO vo : vos) {
            vo.setDeptName(deptNames.get(vo.getDeptId()));
            List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(vo.getId());
            vo.setRoleIds(roleIds == null ? new ArrayList<>() : roleIds);
            if (!vo.getRoleIds().isEmpty()) {
                List<Role> roles = roleMapper.selectBatchIds(vo.getRoleIds());
                vo.setRoleNames(roles.stream().map(Role::getName).toList());
            } else {
                vo.setRoleNames(List.of());
            }
        }
    }
}
