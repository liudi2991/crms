package com.company.crms.payment.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.response.Result;
import com.company.crms.payment.dto.CreatePlanDTO;
import com.company.crms.payment.dto.GeneratePlansDTO;
import com.company.crms.payment.dto.PlanQuery;
import com.company.crms.payment.dto.UpdatePlanDTO;
import com.company.crms.payment.service.PaymentPlanService;
import com.company.crms.payment.vo.PaymentPlanVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "回款计划")
@RestController
@RequestMapping("/api/v1/payment-plans")
@RequiredArgsConstructor
public class PaymentPlanController {

    private final PaymentPlanService service;

    @Operation(summary = "回款计划列表（分页）")
    @SaCheckPermission("payment:plan")
    @GetMapping
    public Result<PageResult<PaymentPlanVO>> list(@Valid PlanQuery query) {
        return Result.ok(service.page(query));
    }

    @Operation(summary = "合同下的回款计划")
    @SaCheckPermission("payment:plan")
    @GetMapping("/by-contract/{contractId}")
    public Result<List<PaymentPlanVO>> byContract(@PathVariable Long contractId) {
        return Result.ok(service.listByContract(contractId));
    }

    @Operation(summary = "新增回款计划")
    @SaCheckPermission("payment:plan")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreatePlanDTO dto) {
        return Result.ok(service.create(dto));
    }

    @Operation(summary = "批量生成回款计划")
    @SaCheckPermission("payment:plan")
    @PostMapping("/generate")
    public Result<List<Long>> generate(@Valid @RequestBody GeneratePlansDTO dto) {
        return Result.ok(service.generate(dto));
    }

    @Operation(summary = "修改回款计划")
    @SaCheckPermission("payment:plan")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdatePlanDTO dto) {
        service.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除回款计划")
    @SaCheckPermission("payment:plan")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.remove(id);
        return Result.ok();
    }
}
