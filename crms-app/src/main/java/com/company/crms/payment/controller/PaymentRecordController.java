package com.company.crms.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.response.Result;
import com.company.crms.payment.dto.CreateRecordDTO;
import com.company.crms.payment.dto.ManualSettleDTO;
import com.company.crms.payment.dto.RecordQuery;
import com.company.crms.payment.dto.RedReverseDTO;
import com.company.crms.payment.service.PaymentRecordService;
import com.company.crms.payment.service.RedReverseService;
import com.company.crms.payment.vo.ImportResultVO;
import com.company.crms.payment.service.PaymentImportService;
import com.company.crms.payment.vo.PaymentRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "实际回款")
@RestController
@RequestMapping("/api/v1/payment-records")
@RequiredArgsConstructor
public class PaymentRecordController {

    private final PaymentRecordService service;
    private final RedReverseService redReverseService;
    private final PaymentImportService importService;

    @Operation(summary = "实际回款列表（分页）")
    @SaCheckPermission("payment:record")
    @GetMapping
    public Result<PageResult<PaymentRecordVO>> list(@Valid RecordQuery query) {
        return Result.ok(service.page(query));
    }

    @Operation(summary = "实际回款详情")
    @SaCheckPermission("payment:record")
    @GetMapping("/{id}")
    public Result<PaymentRecordVO> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @Operation(summary = "合同下的实际回款记录")
    @SaCheckPermission("payment:record")
    @GetMapping("/by-contract/{contractId}")
    public Result<List<PaymentRecordVO>> byContract(@PathVariable Long contractId) {
        return Result.ok(service.listByContract(contractId));
    }

    @Operation(summary = "登记实际回款（含自动核销）")
    @SaCheckPermission("payment:record")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateRecordDTO dto) {
        return Result.ok(service.create(dto));
    }

    @Operation(summary = "手工核销")
    @SaCheckPermission("payment:settle")
    @PostMapping("/manual-settle")
    public Result<Void> manualSettle(@Valid @RequestBody ManualSettleDTO dto) {
        service.manualSettle(dto);
        return Result.ok();
    }

    @Operation(summary = "红冲")
    @SaCheckPermission("payment:red")
    @PostMapping("/{id}/red-reverse")
    public Result<Long> redReverse(@PathVariable Long id, @Valid @RequestBody RedReverseDTO dto) {
        return Result.ok(redReverseService.redReverse(id, dto.getRedAmount(), dto.getReason()));
    }

    @Operation(summary = "软删除实际回款")
    @SaCheckPermission("payment:record")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.remove(id);
        return Result.ok();
    }

    @Operation(summary = "硬删除实际回款（仅超管）")
    @SaCheckPermission("payment:hard_delete")
    @DeleteMapping("/{id}/hard")
    public Result<Void> hardDelete(@PathVariable Long id, @RequestParam(required = false) String reason) {
        service.hardDelete(id, reason);
        return Result.ok();
    }

    @Operation(summary = "Excel 批量导入实际回款")
    @SaCheckPermission("payment:import")
    @PostMapping("/import")
    public Result<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.ok(importService.importRecords(file));
    }
}
