package com.company.crms.iam.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.Result;
import com.company.crms.iam.dto.CreateRoleDTO;
import com.company.crms.iam.dto.UpdateRoleDTO;
import com.company.crms.iam.service.RoleAdminService;
import com.company.crms.iam.vo.RoleVO;
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

import java.util.List;

@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Validated
public class RoleController {

    private final RoleAdminService service;

    @Operation(summary = "角色列表")
    @SaCheckPermission("system:role")
    @GetMapping
    public Result<List<RoleVO>> list() {
        return Result.ok(service.list());
    }

    @Operation(summary = "角色详情")
    @SaCheckPermission("system:role")
    @GetMapping("/{id}")
    public Result<RoleVO> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @Operation(summary = "新建角色")
    @SaCheckPermission("system:role")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateRoleDTO dto) {
        return Result.ok(service.create(dto));
    }

    @Operation(summary = "更新角色")
    @SaCheckPermission("system:role")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateRoleDTO dto) {
        service.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除角色（仅自定义角色）")
    @SaCheckPermission("system:role")
    @DeleteMapping("/{id}")
    public Result<Void> softDelete(@PathVariable Long id) {
        service.softDelete(id);
        return Result.ok();
    }
}
