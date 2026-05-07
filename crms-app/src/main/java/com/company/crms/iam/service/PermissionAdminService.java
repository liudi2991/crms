package com.company.crms.iam.service;

import com.company.crms.iam.vo.PermissionVO;

import java.util.List;

public interface PermissionAdminService {

    /** 全量权限点（按 parent_code 组织成树）。 */
    List<PermissionVO> tree();
}
