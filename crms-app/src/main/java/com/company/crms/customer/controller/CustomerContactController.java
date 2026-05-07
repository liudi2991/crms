package com.company.crms.customer.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.Result;
import com.company.crms.customer.dto.CreateContactDTO;
import com.company.crms.customer.dto.UpdateContactDTO;
import com.company.crms.customer.service.CustomerContactService;
import com.company.crms.customer.vo.CustomerContactVO;
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

@Tag(name = "客户联系人")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class CustomerContactController {

    private final CustomerContactService service;

    @Operation(summary = "客户联系人列表")
    @SaCheckPermission("customer:list")
    @GetMapping("/customers/{customerId}/contacts")
    public Result<List<CustomerContactVO>> list(@PathVariable Long customerId) {
        return Result.ok(service.listByCustomer(customerId));
    }

    @Operation(summary = "新建联系人")
    @SaCheckPermission("customer:update")
    @PostMapping("/contacts")
    public Result<Long> create(@Valid @RequestBody CreateContactDTO dto) {
        return Result.ok(service.create(dto));
    }

    @Operation(summary = "更新联系人")
    @SaCheckPermission("customer:update")
    @PutMapping("/contacts/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateContactDTO dto) {
        service.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "删除联系人")
    @SaCheckPermission("customer:update")
    @DeleteMapping("/contacts/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        service.remove(id);
        return Result.ok();
    }

    @Operation(summary = "设为主联系人")
    @SaCheckPermission("customer:update")
    @PostMapping("/contacts/{id}/primary")
    public Result<Void> setPrimary(@PathVariable Long id) {
        service.setPrimary(id);
        return Result.ok();
    }
}
