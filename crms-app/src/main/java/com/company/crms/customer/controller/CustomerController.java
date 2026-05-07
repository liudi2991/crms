package com.company.crms.customer.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.response.Result;
import com.company.crms.customer.dto.CreateCustomerDTO;
import com.company.crms.customer.dto.CustomerQuery;
import com.company.crms.customer.dto.MergeCustomerDTO;
import com.company.crms.customer.dto.UpdateCustomerDTO;
import com.company.crms.customer.service.CustomerService;
import com.company.crms.customer.vo.CustomerAggregateVO;
import com.company.crms.customer.vo.CustomerDuplicateVO;
import com.company.crms.customer.vo.CustomerVO;
import com.company.crms.system.entity.ChangeLog;
import com.company.crms.system.service.ChangeLogService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "客户管理")
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Validated
public class CustomerController {

    private final CustomerService service;
    private final ChangeLogService changeLogService;

    @Operation(summary = "客户列表（分页 + 关键词 + 范围保护）")
    @SaCheckPermission("customer:list")
    @GetMapping
    public Result<PageResult<CustomerVO>> list(@Valid CustomerQuery query) {
        return Result.ok(service.page(query));
    }

    @Operation(summary = "客户详情")
    @SaCheckPermission("customer:list")
    @GetMapping("/{id}")
    public Result<CustomerVO> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @Operation(summary = "新建客户")
    @SaCheckPermission("customer:create")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateCustomerDTO dto) {
        return Result.ok(service.create(dto));
    }

    @Operation(summary = "更新客户")
    @SaCheckPermission("customer:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateCustomerDTO dto) {
        dto.setId(id);
        service.update(dto);
        return Result.ok();
    }

    @Operation(summary = "软删除客户")
    @SaCheckPermission("customer:delete")
    @DeleteMapping("/{id}")
    public Result<Void> softDelete(@PathVariable Long id) {
        service.softDelete(id);
        return Result.ok();
    }

    @Operation(summary = "硬删除客户（仅超管）")
    @SaCheckPermission("customer:hard_delete")
    @DeleteMapping("/{id}/hard")
    public Result<Void> hardDelete(@PathVariable Long id,
                                   @Parameter(description = "硬删除原因") @RequestParam(required = false) String reason) {
        service.hardDelete(id, reason);
        return Result.ok();
    }

    @Operation(summary = "停用客户")
    @SaCheckPermission("customer:disable")
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id) {
        service.disable(id);
        return Result.ok();
    }

    @Operation(summary = "启用客户")
    @SaCheckPermission("customer:disable")
    @PostMapping("/{id}/enable")
    public Result<Void> enable(@PathVariable Long id) {
        service.enable(id);
        return Result.ok();
    }

    @Operation(summary = "合并客户")
    @SaCheckPermission("customer:merge")
    @PostMapping("/merge")
    public Result<Void> merge(@Valid @RequestBody MergeCustomerDTO dto) {
        service.merge(dto);
        return Result.ok();
    }

    @Operation(summary = "客户查重")
    @SaCheckPermission("customer:list")
    @GetMapping("/check-duplicate")
    public Result<List<CustomerDuplicateVO>> checkDuplicate(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String uscc,
            @RequestParam(required = false) Long selfId) {
        return Result.ok(service.checkDuplicate(name, uscc, selfId));
    }

    @Operation(summary = "详情聚合（基础信息 + 联系人 + 最近合同 + 变更）")
    @SaCheckPermission("customer:list")
    @GetMapping("/{id}/aggregate")
    public Result<CustomerAggregateVO> aggregate(@PathVariable Long id) {
        return Result.ok(service.aggregate(id));
    }

    @Operation(summary = "客户变更记录")
    @SaCheckPermission("customer:list")
    @GetMapping("/{id}/changes")
    public Result<List<ChangeLog>> changes(@PathVariable Long id,
                                           @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(changeLogService.listByBiz("CUSTOMER", id, limit));
    }
}
