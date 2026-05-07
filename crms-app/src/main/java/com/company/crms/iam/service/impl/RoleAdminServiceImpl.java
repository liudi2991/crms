package com.company.crms.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.iam.dto.CreateRoleDTO;
import com.company.crms.iam.dto.UpdateRoleDTO;
import com.company.crms.iam.entity.Permission;
import com.company.crms.iam.entity.Role;
import com.company.crms.iam.mapper.PermissionMapper;
import com.company.crms.iam.mapper.RoleMapper;
import com.company.crms.iam.mapper.RolePermissionMapper;
import com.company.crms.iam.mapper.UserRoleMapper;
import com.company.crms.iam.service.RoleAdminService;
import com.company.crms.iam.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleAdminServiceImpl implements RoleAdminService {

    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final UserRoleMapper userRoleMapper;

    @Override
    public List<RoleVO> list() {
        QueryWrapper<Role> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByAsc("id");
        return roleMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public RoleVO detail(Long id) {
        Role r = roleMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        return toVO(r);
    }

    @Override
    @OperationLog(module = "角色", action = "新建角色", type = "CREATE")
    @Transactional
    public Long create(CreateRoleDTO dto) {
        validatePermissions(dto.getPermissionCodes());
        Role r = new Role();
        r.setId(SnowflakeIdGenerator.next());
        r.setCode(dto.getCode());
        r.setName(dto.getName());
        r.setDataScope(dto.getDataScope());
        r.setDescription(dto.getDescription());
        r.setBuiltin(0);
        r.setSort(dto.getSort() == null ? 0 : dto.getSort());
        try {
            roleMapper.insert(r);
        } catch (DuplicateKeyException e) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "角色编码已存在");
        }
        replacePermissions(r.getId(), dto.getPermissionCodes());
        return r.getId();
    }

    @Override
    @OperationLog(module = "角色", action = "更新角色", type = "UPDATE")
    @Transactional
    public void update(Long id, UpdateRoleDTO dto) {
        Role r = roleMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        validatePermissions(dto.getPermissionCodes());
        Role upd = new Role();
        upd.setId(id);
        upd.setName(dto.getName());
        upd.setDataScope(dto.getDataScope());
        upd.setDescription(dto.getDescription());
        if (dto.getSort() != null) {
            upd.setSort(dto.getSort());
        }
        upd.setVersion(dto.getVersion());
        int rows = roleMapper.updateById(upd);
        if (rows == 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "角色已被其它用户修改，请刷新后重试");
        }
        replacePermissions(id, dto.getPermissionCodes());
    }

    @Override
    @OperationLog(module = "角色", action = "删除角色", type = "DELETE")
    @Transactional
    public void softDelete(Long id) {
        Role r = roleMapper.selectById(id);
        if (r == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "角色不存在");
        }
        if (r.getBuiltin() != null && r.getBuiltin() == 1) {
            throw new BizException(ErrorCode.AUTH_FORBIDDEN, "内置角色不可删除");
        }
        if (userRoleMapper.countByRoleId(id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "角色已分配给用户，无法删除");
        }
        roleMapper.deleteById(id);
        rolePermissionMapper.deleteByRoleId(id);
    }

    private void validatePermissions(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return;
        }
        Set<String> requested = new HashSet<>(codes);
        Set<String> existing = permissionMapper.selectList(null).stream()
                .map(Permission::getCode).collect(Collectors.toSet());
        for (String c : requested) {
            if (!existing.contains(c)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "存在未知权限点: " + c);
            }
        }
    }

    private void replacePermissions(Long roleId, List<String> codes) {
        rolePermissionMapper.deleteByRoleId(roleId);
        if (codes != null) {
            for (String code : new HashSet<>(codes)) {
                rolePermissionMapper.insert(SnowflakeIdGenerator.next(), roleId, code);
            }
        }
    }

    private RoleVO toVO(Role r) {
        RoleVO vo = new RoleVO();
        vo.setId(r.getId());
        vo.setCode(r.getCode());
        vo.setName(r.getName());
        vo.setDataScope(r.getDataScope());
        vo.setDescription(r.getDescription());
        vo.setBuiltin(r.getBuiltin() != null && r.getBuiltin() == 1);
        vo.setSort(r.getSort());
        vo.setVersion(r.getVersion());
        vo.setPermissionCodes(rolePermissionMapper.selectCodesByRoleId(r.getId()));
        vo.setUserCount(userRoleMapper.countByRoleId(r.getId()));
        return vo;
    }
}
