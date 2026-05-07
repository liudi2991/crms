package com.company.crms.iam.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.response.Result;
import com.company.crms.iam.dto.AssignRolesDTO;
import com.company.crms.iam.dto.CreateUserDTO;
import com.company.crms.iam.dto.ResetPasswordDTO;
import com.company.crms.iam.dto.UpdateUserDTO;
import com.company.crms.iam.dto.UserQuery;
import com.company.crms.iam.service.UserAdminService;
import com.company.crms.iam.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserAdminService service;

    @Operation(summary = "用户分页列表")
    @SaCheckPermission("system:user")
    @GetMapping
    public Result<PageResult<UserVO>> list(@Valid UserQuery query) {
        return Result.ok(service.page(query));
    }

    @Operation(summary = "用户详情")
    @SaCheckPermission("system:user")
    @GetMapping("/{id}")
    public Result<UserVO> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @Operation(summary = "新建用户")
    @SaCheckPermission("system:user")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateUserDTO dto) {
        return Result.ok(service.create(dto));
    }

    @Operation(summary = "更新用户基础信息")
    @SaCheckPermission("system:user")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        service.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "软删除用户")
    @SaCheckPermission("system:user")
    @DeleteMapping("/{id}")
    public Result<Void> softDelete(@PathVariable Long id) {
        service.softDelete(id);
        return Result.ok();
    }

    @Operation(summary = "重置用户密码")
    @SaCheckPermission("system:user")
    @PostMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordDTO dto) {
        service.resetPassword(id, dto);
        return Result.ok();
    }

    @Operation(summary = "为用户分配角色")
    @SaCheckPermission("system:user")
    @PostMapping("/{id}/roles")
    public Result<Void> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesDTO dto) {
        service.assignRoles(id, dto);
        return Result.ok();
    }

    @Operation(summary = "停用用户")
    @SaCheckPermission("system:user")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        service.disable(id);
        return Result.ok();
    }

    @Operation(summary = "启用用户")
    @SaCheckPermission("system:user")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        service.enable(id);
        return Result.ok();
    }

    @Operation(summary = "解锁用户（清失败次数与锁定截止）")
    @SaCheckPermission("system:user")
    @PostMapping("/{id}/unlock")
    public Result<Void> unlock(@PathVariable Long id) {
        service.unlock(id);
        return Result.ok();
    }
}
