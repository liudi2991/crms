package com.company.crms.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.response.Result;
import com.company.crms.system.dto.OperationLogQuery;
import com.company.crms.system.entity.OperationLogEntity;
import com.company.crms.system.service.OperationLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/v1/operation-logs")
@RequiredArgsConstructor
@Validated
public class OperationLogController {

    private final OperationLogQueryService service;

    @Operation(summary = "操作日志分页")
    @SaCheckPermission("system:log")
    @GetMapping
    public Result<PageResult<OperationLogEntity>> list(@Valid OperationLogQuery query) {
        return Result.ok(service.page(query));
    }
}
