package com.company.crms.iam.service;

import com.company.crms.iam.dto.CreateRoleDTO;
import com.company.crms.iam.dto.UpdateRoleDTO;
import com.company.crms.iam.vo.RoleVO;

import java.util.List;

public interface RoleAdminService {

    List<RoleVO> list();

    RoleVO detail(Long id);

    Long create(CreateRoleDTO dto);

    void update(Long id, UpdateRoleDTO dto);

    void softDelete(Long id);
}
