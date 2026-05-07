package com.company.crms.iam.service;

import com.company.crms.common.response.PageResult;
import com.company.crms.iam.dto.AssignRolesDTO;
import com.company.crms.iam.dto.CreateUserDTO;
import com.company.crms.iam.dto.ResetPasswordDTO;
import com.company.crms.iam.dto.UpdateUserDTO;
import com.company.crms.iam.dto.UserQuery;
import com.company.crms.iam.vo.UserVO;

public interface UserAdminService {

    PageResult<UserVO> page(UserQuery query);

    UserVO detail(Long id);

    Long create(CreateUserDTO dto);

    void update(Long id, UpdateUserDTO dto);

    void softDelete(Long id);

    void resetPassword(Long id, ResetPasswordDTO dto);

    void assignRoles(Long id, AssignRolesDTO dto);

    void disable(Long id);

    void enable(Long id);

    void unlock(Long id);
}
