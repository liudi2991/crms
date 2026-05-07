package com.company.crms.contract.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.common.response.Result;
import com.company.crms.contract.entity.ContractAttachment;
import com.company.crms.contract.mapper.ContractAttachmentMapper;
import com.company.crms.contract.service.ContractAttachmentService;
import com.company.crms.contract.vo.ContractAttachmentVO;
import com.company.crms.file.service.FileStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "合同附件")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ContractAttachmentController {

    private final ContractAttachmentService service;
    private final ContractAttachmentMapper attachmentMapper;
    private final FileStorage storage;

    @Operation(summary = "合同附件列表")
    @SaCheckPermission("contract:list")
    @GetMapping("/contracts/{contractId}/attachments")
    public Result<List<ContractAttachmentVO>> list(@PathVariable Long contractId) {
        QueryWrapper<ContractAttachment> w = new QueryWrapper<ContractAttachment>()
                .eq("contract_id", contractId)
                .orderByDesc("uploaded_at");
        List<ContractAttachmentVO> list = attachmentMapper.selectList(w).stream().map(a -> {
            ContractAttachmentVO vo = new ContractAttachmentVO();
            BeanUtils.copyProperties(a, vo);
            vo.setPreviewUrl(storage.previewUrl(a.getFileObjectId(), 600));
            return vo;
        }).toList();
        return Result.ok(list);
    }

    @Operation(summary = "上传合同附件")
    @SaCheckPermission("contract:update")
    @PostMapping("/contracts/{contractId}/attachments")
    public Result<Long> upload(@PathVariable Long contractId, @RequestParam("file") MultipartFile file) {
        return Result.ok(service.upload(contractId, file));
    }

    @Operation(summary = "删除合同附件")
    @SaCheckPermission("contract:update")
    @DeleteMapping("/attachments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.ok();
    }
}
