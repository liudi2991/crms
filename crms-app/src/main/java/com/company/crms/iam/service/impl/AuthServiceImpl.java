package com.company.crms.iam.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.security.UserContext;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.iam.dto.ChangePasswordRequest;
import com.company.crms.iam.dto.LoginRequest;
import com.company.crms.iam.entity.User;
import com.company.crms.iam.mapper.UserMapper;
import com.company.crms.iam.service.AuthService;
import com.company.crms.iam.service.UserContextLoader;
import com.company.crms.iam.vo.LoginVO;
import com.company.crms.iam.vo.MeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import cn.hutool.crypto.digest.BCrypt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final UserContextLoader contextLoader;

    @Value("${crms.security.login-max-failed:5}")
    private int loginMaxFailed;

    @Value("${crms.security.login-lock-minutes:15}")
    private int loginLockMinutes;

    @Override
    @OperationLog(module = "认证", action = "登录", type = "LOGIN", recordParams = false)
    public LoginVO login(LoginRequest req, String ip) {
        User user = userMapper.selectByUsername(req.getUsername());
        if (user == null) {
            throw new BizException(ErrorCode.IAM_LOGIN_FAILED);
        }
        if ("DISABLED".equals(user.getStatus())) {
            throw new BizException(ErrorCode.IAM_ACCOUNT_DISABLED);
        }
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BizException(ErrorCode.IAM_ACCOUNT_LOCKED,
                    "锁定至 " + user.getLockedUntil());
        }

        boolean ok = BCrypt.checkpw(req.getPassword(), user.getPasswordHash());
        if (!ok) {
            int failed = (user.getFailedCount() == null ? 0 : user.getFailedCount()) + 1;
            User upd = new User();
            upd.setId(user.getId());
            upd.setFailedCount(failed);
            if (failed >= loginMaxFailed) {
                upd.setLockedUntil(LocalDateTime.now().plus(Duration.ofMinutes(loginLockMinutes)));
                upd.setFailedCount(0);
                userMapper.updateById(upd);
                throw new BizException(ErrorCode.IAM_ACCOUNT_LOCKED,
                        "失败次数过多，锁定 " + loginLockMinutes + " 分钟");
            }
            userMapper.updateById(upd);
            throw new BizException(ErrorCode.IAM_LOGIN_FAILED);
        }

        // 登录成功：更新最后登录时间，清零失败次数
        User reset = new User();
        reset.setId(user.getId());
        reset.setFailedCount(0);
        reset.setLockedUntil(null);
        reset.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(reset);

        StpUtil.login(user.getId());
        contextLoader.invalidate(user.getId());

        LoginVO vo = new LoginVO();
        vo.setToken(StpUtil.getTokenValue());
        vo.setForceChangePassword(user.getMustChangePwd() != null && user.getMustChangePwd() == 1);
        log.info("user {} login from {}", user.getUsername(), ip);
        return vo;
    }

    @Override
    @OperationLog(module = "认证", action = "登出", type = "LOGIN")
    public void logout() {
        if (StpUtil.isLogin()) {
            Long uid = StpUtil.getLoginIdAsLong();
            StpUtil.logout();
            contextLoader.invalidate(uid);
        }
    }

    @Override
    public MeVO me() {
        UserContext ctx = UserContextHolder.require();
        MeVO me = new MeVO();
        me.setId(ctx.getUserId());
        me.setUsername(ctx.getUsername());
        me.setRealName(ctx.getRealName());
        me.setDeptId(ctx.getDeptId());
        me.setRoles(ctx.getRoles());
        me.setPermissions(ctx.getPermissions().stream().toList());
        me.setSuperAdmin(ctx.isSuperAdmin());
        User u = userMapper.selectById(ctx.getUserId());
        me.setForceChangePassword(u != null && u.getMustChangePwd() != null && u.getMustChangePwd() == 1);
        return me;
    }

    @Override
    @OperationLog(module = "认证", action = "修改密码", type = "UPDATE", recordParams = false)
    public void changePassword(ChangePasswordRequest req) {
        Long uid = UserContextHolder.currentUserId();
        User user = userMapper.selectById(uid);
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        if (!BCrypt.checkpw(req.getOldPassword(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.IAM_OLD_PASSWORD_INCORRECT);
        }
        if (!isStrongPassword(req.getNewPassword())) {
            throw new BizException(ErrorCode.IAM_PASSWORD_WEAK,
                    "需要包含大小写字母与数字，长度 ≥ 8");
        }
        User upd = new User();
        upd.setId(uid);
        upd.setPasswordHash(BCrypt.hashpw(req.getNewPassword(), BCrypt.gensalt(10)));
        upd.setMustChangePwd(0);
        userMapper.updateById(upd);
        contextLoader.invalidate(uid);
    }

    @Override
    public boolean verifyPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        Long uid = UserContextHolder.currentUserId();
        if (uid == null) {
            return false;
        }
        User user = userMapper.selectById(uid);
        if (user == null) {
            return false;
        }
        return BCrypt.checkpw(password, user.getPasswordHash());
    }

    private boolean isStrongPassword(String pwd) {
        if (pwd == null || pwd.length() < 8) {
            return false;
        }
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : pwd.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        return hasUpper && hasLower && hasDigit;
    }
}
