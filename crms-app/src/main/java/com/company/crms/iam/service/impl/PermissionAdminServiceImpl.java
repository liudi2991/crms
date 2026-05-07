package com.company.crms.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.iam.entity.Permission;
import com.company.crms.iam.mapper.PermissionMapper;
import com.company.crms.iam.service.PermissionAdminService;
import com.company.crms.iam.vo.PermissionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PermissionAdminServiceImpl implements PermissionAdminService {

    private final PermissionMapper permissionMapper;

    @Override
    public List<PermissionVO> tree() {
        QueryWrapper<Permission> wrapper = new QueryWrapper<>();
        wrapper.orderByAsc("sort", "id");
        List<Permission> all = permissionMapper.selectList(wrapper);

        Map<String, PermissionVO> byCode = new HashMap<>();
        List<PermissionVO> roots = new ArrayList<>();
        for (Permission p : all) {
            PermissionVO vo = new PermissionVO();
            BeanUtils.copyProperties(p, vo);
            byCode.put(vo.getCode(), vo);
        }
        // 按 parent_code 组装；未指明父节点的归到顶层（按 type=MENU 与无父分组）
        for (Permission p : all) {
            PermissionVO vo = byCode.get(p.getCode());
            if (p.getParentCode() != null && byCode.containsKey(p.getParentCode())) {
                byCode.get(p.getParentCode()).getChildren().add(vo);
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }
}
