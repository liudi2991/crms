package com.company.crms.iam.service;

import com.company.crms.iam.dto.CreateDepartmentDTO;
import com.company.crms.iam.dto.UpdateDepartmentDTO;
import com.company.crms.iam.vo.DepartmentVO;

import java.util.List;

public interface DepartmentAdminService {

    List<DepartmentVO> tree();

    DepartmentVO detail(Long id);

    Long create(CreateDepartmentDTO dto);

    void update(Long id, UpdateDepartmentDTO dto);

    void softDelete(Long id);
}
