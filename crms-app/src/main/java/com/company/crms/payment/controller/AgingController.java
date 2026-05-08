package com.company.crms.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.Result;
import com.company.crms.payment.service.AgingService;
import com.company.crms.payment.vo.AgingBucketVO;
import com.company.crms.payment.vo.AgingDrillVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "账龄分析")
@RestController
@RequestMapping("/api/v1/aging")
@RequiredArgsConstructor
public class AgingController {

    private final AgingService agingService;

    @Operation(summary = "账龄桶（UNDUE / 0-30 / 31-60 / 61-90 / 90+）")
    // 历史脏数据：早期写成 report:aging，但 V1.0.1 seed 里没有这个权限点，导致除 superAdmin
    // 外的所有角色都拿不到账龄数据。现在统一回 report:payment（同 /reports/aging）。
    @SaCheckPermission("report:payment")
    @GetMapping
    public Result<List<AgingBucketVO>> aging(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today) {
        return Result.ok(agingService.aging(today == null ? LocalDate.now() : today));
    }

    @Operation(summary = "账龄钻取：列出某桶下的回款计划")
    @SaCheckPermission("report:payment")
    @GetMapping("/drill")
    public Result<List<AgingDrillVO>> drill(
            @RequestParam String bucket,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate today,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        return Result.ok(agingService.drill(today == null ? LocalDate.now() : today, bucket, page, size));
    }
}
