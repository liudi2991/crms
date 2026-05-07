package com.company.crms.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.iam.dto.CreateDepartmentDTO;
import com.company.crms.iam.dto.UpdateDepartmentDTO;
import com.company.crms.iam.entity.Department;
import com.company.crms.iam.mapper.DepartmentMapper;
import com.company.crms.iam.service.DepartmentAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.company.crms.iam.vo.DepartmentVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DepartmentAdminServiceImpl implements DepartmentAdminService {

    private final DepartmentMapper departmentMapper;

    @Override
    public List<DepartmentVO> tree() {
        QueryWrapper<Department> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort").orderByAsc("id");
        List<Department> all = departmentMapper.selectList(wrapper);

        Map<Long, DepartmentVO> byId = new HashMap<>();
        for (Department d : all) {
            DepartmentVO vo = toVO(d);
            byId.put(vo.getId(), vo);
        }
        List<DepartmentVO> roots = new ArrayList<>();
        for (Department d : all) {
            DepartmentVO vo = byId.get(d.getId());
            Long pid = d.getParentId() == null ? 0L : d.getParentId();
            if (pid == 0L || !byId.containsKey(pid)) {
                roots.add(vo);
            } else {
                byId.get(pid).getChildren().add(vo);
            }
        }
        return roots;
    }

    @Override
    public DepartmentVO detail(Long id) {
        Department d = departmentMapper.selectById(id);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        return toVO(d);
    }

    @Override
    @OperationLog(module = "部门", action = "新建部门", type = "CREATE")
    @Transactional
    public Long create(CreateDepartmentDTO dto) {
        Long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        String parentPath = "";
        if (parentId != 0L) {
            Department parent = departmentMapper.selectById(parentId);
            if (parent == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "父部门不存在");
            }
            parentPath = parent.getFullPath();
        }
        Department d = new Department();
        d.setId(SnowflakeIdGenerator.next());
        d.setParentId(parentId);
        d.setName(dto.getName());
        d.setSort(dto.getSort() == null ? 0 : dto.getSort());
        d.setFullPath(parentPath + "/" + dto.getName());
        departmentMapper.insert(d);
        return d.getId();
    }

    @Override
    @OperationLog(module = "部门", action = "更新部门", type = "UPDATE")
    @Transactional
    public void update(Long id, UpdateDepartmentDTO dto) {
        Department existing = departmentMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        Long newParentId = dto.getParentId() == null ? 0L : dto.getParentId();
        if (Objects.equals(newParentId, id)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "不能将部门设为自己的子部门");
        }
        // 防循环：新父不能是自身的子孙
        if (newParentId != 0L) {
            List<Long> descendants = departmentMapper.selectSelfAndDescendantIds(id);
            if (descendants != null && descendants.contains(newParentId)) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "不能将部门移动到自己的子孙下");
            }
        }
        String parentPath = "";
        if (newParentId != 0L) {
            Department parent = departmentMapper.selectById(newParentId);
            if (parent == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "父部门不存在");
            }
            parentPath = parent.getFullPath();
        }
        Department upd = new Department();
        upd.setId(id);
        upd.setParentId(newParentId);
        upd.setName(dto.getName());
        upd.setSort(dto.getSort());
        upd.setFullPath(parentPath + "/" + dto.getName());
        upd.setVersion(dto.getVersion());
        int rows = departmentMapper.updateById(upd);
        if (rows == 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "部门已被其它用户修改，请刷新后重试");
        }
        // 注意：fullPath 链上的子孙节点路径未在此处级联更新，受 Sprint 范围限制；
        // 后续 S3 引入 ChangeLog 时一并补级联更新。
    }

    @Override
    @OperationLog(module = "部门", action = "删除部门", type = "DELETE")
    @Transactional
    public void softDelete(Long id) {
        Department d = departmentMapper.selectById(id);
        if (d == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "部门不存在");
        }
        if (departmentMapper.countDirectChildren(id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "存在子部门，请先删除子部门");
        }
        if (departmentMapper.countUsersInDept(id) > 0) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "部门下仍有用户，无法删除");
        }
        departmentMapper.deleteById(id);
    }

    private DepartmentVO toVO(Department d) {
        DepartmentVO vo = new DepartmentVO();
        vo.setId(d.getId());
        vo.setParentId(d.getParentId());
        vo.setName(d.getName());
        vo.setFullPath(d.getFullPath());
        vo.setSort(d.getSort());
        vo.setVersion(d.getVersion());
        vo.setUserCount(departmentMapper.countUsersInDept(d.getId()));
        return vo;
    }
}
