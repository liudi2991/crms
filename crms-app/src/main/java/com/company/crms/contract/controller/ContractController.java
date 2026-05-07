package com.company.crms.contract.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.response.Result;
import com.company.crms.contract.dto.ContractQuery;
import com.company.crms.contract.dto.CreateContractDTO;
import com.company.crms.contract.dto.TransitionDTO;
import com.company.crms.contract.dto.UpdateContractDTO;
import com.company.crms.contract.entity.ContractNote;
import com.company.crms.contract.enums.ContractStatus;
import com.company.crms.contract.service.ContractNoteService;
import com.company.crms.contract.service.ContractService;
import com.company.crms.contract.vo.ContractVO;
import com.company.crms.system.entity.ChangeLog;
import com.company.crms.system.service.ChangeLogService;
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

import java.util.List;
import java.util.Map;

@Tag(name = "合同管理")
@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
@Validated
public class ContractController {

    private final ContractService service;
    private final ContractNoteService noteService;
    private final ChangeLogService changeLogService;

    @Operation(summary = "合同列表（分页 + 范围保护）")
    @SaCheckPermission("contract:list")
    @GetMapping
    public Result<PageResult<ContractVO>> list(@Valid ContractQuery query) {
        return Result.ok(service.page(query));
    }

    @Operation(summary = "合同详情")
    @SaCheckPermission("contract:list")
    @GetMapping("/{id}")
    public Result<ContractVO> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @Operation(summary = "新建合同")
    @SaCheckPermission("contract:create")
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreateContractDTO dto) {
        return Result.ok(service.create(dto));
    }

    @Operation(summary = "更新合同")
    @SaCheckPermission("contract:update")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateContractDTO dto) {
        service.update(id, dto);
        return Result.ok();
    }

    @Operation(summary = "软删除合同")
    @SaCheckPermission("contract:delete")
    @DeleteMapping("/{id}")
    public Result<Void> softDelete(@PathVariable Long id) {
        service.softDelete(id);
        return Result.ok();
    }

    @Operation(summary = "硬删除合同（仅超管）")
    @SaCheckPermission("contract:hard_delete")
    @DeleteMapping("/{id}/hard")
    public Result<Void> hardDelete(@PathVariable Long id,
                                   @Parameter(description = "硬删除原因") @RequestParam(required = false) String reason) {
        service.hardDelete(id, reason);
        return Result.ok();
    }

    @Operation(summary = "状态流转")
    @SaCheckPermission("contract:update")
    @PostMapping("/{id}/transition")
    public Result<Void> transition(@PathVariable Long id, @Valid @RequestBody TransitionDTO dto) {
        service.transition(id, ContractStatus.of(dto.getTo()), dto.getReason());
        return Result.ok();
    }

    @Operation(summary = "终止合同（DRAFT/EFFECTIVE → TERMINATED）")
    @SaCheckPermission("contract:terminate")
    @PostMapping("/{id}/terminate")
    public Result<Void> terminate(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("reason");
        service.transition(id, ContractStatus.TERMINATED, reason);
        return Result.ok();
    }

    @Operation(summary = "合同变更记录")
    @SaCheckPermission("contract:list")
    @GetMapping("/{id}/changes")
    public Result<List<ChangeLog>> changes(@PathVariable Long id,
                                           @RequestParam(defaultValue = "100") int limit) {
        return Result.ok(changeLogService.listByBiz("CONTRACT", id, limit));
    }

    @Operation(summary = "合同备注列表")
    @SaCheckPermission("contract:list")
    @GetMapping("/{id}/notes")
    public Result<List<ContractNote>> notes(@PathVariable Long id) {
        return Result.ok(noteService.listByContract(id));
    }

    @Operation(summary = "新增合同备注")
    @SaCheckPermission("contract:note")
    @PostMapping("/{id}/notes")
    public Result<Long> addNote(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(noteService.create(id, body == null ? null : body.get("content")));
    }

    @Operation(summary = "删除合同备注")
    @SaCheckPermission("contract:note")
    @DeleteMapping("/notes/{noteId}")
    public Result<Void> deleteNote(@PathVariable Long noteId) {
        noteService.remove(noteId);
        return Result.ok();
    }
}
