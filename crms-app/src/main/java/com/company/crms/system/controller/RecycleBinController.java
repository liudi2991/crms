package com.company.crms.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.company.crms.common.response.PageResult;
import com.company.crms.common.response.Result;
import com.company.crms.system.dto.HardDeleteRequest;
import com.company.crms.system.dto.RecycleBinQuery;
import com.company.crms.system.service.RecycleBinService;
import com.company.crms.system.vo.RecycleBinItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "回收站")
@RestController
@RequestMapping("/api/v1/recycle-bin")
@RequiredArgsConstructor
@Validated
public class RecycleBinController {

    private final RecycleBinService service;

    @Operation(summary = "回收站分页（按 bizType）")
    @SaCheckPermission("system:recycle")
    @GetMapping
    public Result<PageResult<RecycleBinItemVO>> list(@Valid RecycleBinQuery query) {
        return Result.ok(service.page(query));
    }

    @Operation(summary = "还原")
    @SaCheckPermission("system:recycle")
    @PostMapping("/{bizType}/{id}/restore")
    public Result<Void> restore(@PathVariable String bizType, @PathVariable Long id) {
        service.restore(bizType, id);
        return Result.ok();
    }

    @Operation(summary = "硬删除（写 hard_delete_log）")
    @SaCheckPermission("system:recycle")
    @DeleteMapping("/{bizType}/{id}/hard")
    public Result<Void> hardDelete(@PathVariable String bizType,
                                   @PathVariable Long id,
                                   @Valid @RequestBody HardDeleteRequest req) {
        service.hardDelete(bizType, id, req.getReason());
        return Result.ok();
    }
}
