package com.company.crms.iam.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.company.crms.common.response.Result;
import com.company.crms.iam.service.PermissionAdminService;
import com.company.crms.iam.vo.PermissionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "权限点")
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionAdminService service;

    @Operation(summary = "全量权限点（树形）")
    @SaCheckLogin
    @GetMapping("/tree")
    public Result<List<PermissionVO>> tree() {
        return Result.ok(service.tree());
    }
}
