package com.company.crms.contract.service.impl;

import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.security.UserContextHolder;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.contract.entity.ContractAttachment;
import com.company.crms.contract.mapper.ContractAttachmentMapper;
import com.company.crms.contract.mapper.ContractMapper;
import com.company.crms.contract.service.ContractAttachmentService;
import com.company.crms.file.entity.FileObject;
import com.company.crms.file.mapper.FileObjectMapper;
import com.company.crms.file.service.FileStorage;
import com.company.crms.system.service.SystemParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractAttachmentServiceImpl implements ContractAttachmentService {

    private static final long MAX_FILE_BYTES = 50L * 1024 * 1024;
    /** 系统参数键：单合同最大附件数（SRS UC-03-05 默认 20）。 */
    static final String PARAM_MAX_PER_CONTRACT = "contract.attachment.max_count";
    /** 系统参数缺失时的兜底值，与 SRS §4.3 UC-03-05 对齐。 */
    static final int DEFAULT_MAX_PER_CONTRACT = 20;
    private static final Set<String> ALLOWED_EXT = Set.of(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            "jpg", "jpeg", "png", "zip", "rar", "txt"
    );

    private final ContractAttachmentMapper attachmentMapper;
    private final ContractMapper contractMapper;
    private final FileObjectMapper fileObjectMapper;
    private final FileStorage storage;
    private final SystemParamService systemParamService;

    @Override
    @OperationLog(module = "合同", action = "上传附件", recordParams = false)
    @Transactional
    public Long upload(Long contractId, MultipartFile file) {
        if (contractMapper.selectById(contractId) == null) {
            throw new BizException(ErrorCode.CT_NOT_FOUND);
        }
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED, "上传文件为空");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new BizException(ErrorCode.FILE_SIZE_LIMIT);
        }
        String ext = extOf(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new BizException(ErrorCode.CT_ATTACHMENT_TYPE);
        }
        long count = attachmentMapper.countByContract(contractId);
        int maxPerContract = systemParamService.getInt(PARAM_MAX_PER_CONTRACT, DEFAULT_MAX_PER_CONTRACT);
        if (count >= maxPerContract) {
            throw new BizException(ErrorCode.CT_ATTACHMENT_LIMIT);
        }

        String objectKey;
        try {
            objectKey = storage.save("contract", file);
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED, e.getMessage());
        }

        FileObject fo = new FileObject();
        fo.setId(SnowflakeIdGenerator.next());
        fo.setObjectKey(objectKey);
        fo.setBucket(storage.bucket());
        fo.setFileName(file.getOriginalFilename());
        fo.setContentType(file.getContentType());
        fo.setSize(file.getSize());
        fo.setBizType("CONTRACT");
        fo.setBizId(contractId);
        fo.setUploadedBy(UserContextHolder.currentUserId());
        fo.setUploadedAt(LocalDateTime.now());
        fileObjectMapper.insert(fo);

        ContractAttachment a = new ContractAttachment();
        a.setId(SnowflakeIdGenerator.next());
        a.setContractId(contractId);
        a.setFileObjectId(fo.getId());
        a.setFileName(file.getOriginalFilename());
        a.setFileSize(file.getSize());
        a.setUploadedBy(UserContextHolder.currentUserId());
        a.setUploadedAt(LocalDateTime.now());
        attachmentMapper.insert(a);
        return a.getId();
    }

    @Override
    @OperationLog(module = "合同", action = "删除附件", type = "DELETE")
    @Transactional
    public void delete(Long attachmentId) {
        ContractAttachment a = attachmentMapper.selectById(attachmentId);
        if (a == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND, "附件不存在");
        }
        attachmentMapper.deleteById(attachmentId);
        log.info("attachment {} soft-deleted by {}", attachmentId, UserContextHolder.currentUserId());
    }

    @Override
    public String previewUrl(Long attachmentId, int expireSeconds) {
        ContractAttachment a = attachmentMapper.selectById(attachmentId);
        if (a == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        return storage.previewUrl(a.getFileObjectId(), expireSeconds);
    }

    private String extOf(String filename) {
        if (filename == null) return null;
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? null : filename.substring(dot + 1);
    }
}
