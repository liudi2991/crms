package com.company.crms.iam.service;

import com.company.crms.iam.dto.ChangePasswordRequest;
import com.company.crms.iam.dto.LoginRequest;
import com.company.crms.iam.vo.LoginVO;
import com.company.crms.iam.vo.MeVO;

public interface AuthService {
    LoginVO login(LoginRequest req, String ip);

    void logout();

    MeVO me();

    void changePassword(ChangePasswordRequest req);

    /** 校验当前用户密码（用于二次确认高危操作）。 */
    boolean verifyPassword(String password);
}
