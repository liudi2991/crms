package com.company.crms.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.Result;
import com.company.crms.system.dto.UpdateParamDTO;
import com.company.crms.system.entity.SystemParam;
import com.company.crms.system.service.SystemParamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "系统参数")
@RestController
@RequestMapping("/api/v1/system-params")
@RequiredArgsConstructor
@Validated
public class SystemParamController {

    private final SystemParamService service;

    @Operation(summary = "全量参数")
    @SaCheckPermission("system:param")
    @GetMapping
    public Result<List<SystemParam>> list() {
        return Result.ok(service.listAll());
    }

    @Operation(summary = "批量更新参数")
    @SaCheckPermission("system:param")
    @PutMapping("/batch")
    public Result<Void> batchUpdate(@Valid @RequestBody List<UpdateParamDTO> items) {
        for (UpdateParamDTO it : items) {
            service.update(it.getParamKey(), it.getParamValue());
        }
        return Result.ok();
    }
}
