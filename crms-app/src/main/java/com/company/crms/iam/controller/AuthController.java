package com.company.crms.iam.controller;

import com.company.crms.common.response.Result;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.company.crms.iam.dto.ChangePasswordRequest;
import com.company.crms.iam.dto.LoginRequest;
import com.company.crms.iam.dto.VerifyPasswordRequest;
import com.company.crms.iam.service.AuthService;
import java.util.Map;
import com.company.crms.iam.vo.LoginVO;
import com.company.crms.iam.vo.MeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        return Result.ok(authService.login(req, clientIp(http)));
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.ok();
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public Result<MeVO> me() {
        return Result.ok(authService.me());
    }

    @Operation(summary = "修改密码")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(req);
        return Result.ok();
    }

    @Operation(summary = "二次密码校验（用于高危操作前置确认）")
    @SaCheckLogin
    @PostMapping("/verify-password")
    public Result<Map<String, Boolean>> verifyPassword(@Valid @RequestBody VerifyPasswordRequest req) {
        boolean ok = authService.verifyPassword(req.getPassword());
        return Result.ok(Map.of("ok", ok));
    }

    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        return req.getRemoteAddr();
    }
}
