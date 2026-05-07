package com.company.crms.iam.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.Result;
import com.company.crms.iam.dto.CreateDepartmentDTO;
import com.company.crms.iam.dto.UpdateDepartmentDTO;
import com.company.crms.iam.service.DepartmentAdminService;
import com.company.crms.iam.vo.DepartmentVO;
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

@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Validated
public class DepartmentController {

    private final DepartmentAdminService service;

    @Operation(summary = "部门树")
    @SaCheckPermission("system:dept")
    @GetMapping("/tree")
    public Result<List<DepartmentVO>> tree() {
        return Result.ok(service.tree());
    }

    @Operation(summary = "部门详情")
    @SaCheckPermission("system:dept")
    @GetMapping("/{id}")
    public Result<DepartmentVO> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @Operation(summary = "新建部门")
    @SaCheckPermission("system:dept")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateDepartmentDTO dto) {
        return Result.ok(service.create(dto));
    }

    @Operation(summary = "更新部门")
    @SaCheckPermission("system:dept")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateDepartmentDTO dto) {
        service.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除部门")
    @SaCheckPermission("system:dept")
    @DeleteMapping("/{id}")
    public Result<Void> softDelete(@PathVariable Long id) {
        service.softDelete(id);
        return Result.ok();
    }
}
