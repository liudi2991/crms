package com.company.crms.contract.service;

import org.springframework.web.multipart.MultipartFile;

public interface ContractAttachmentService {

    /**
     * 上传附件。
     * <p>校验：合同 ≤ 10 个附件、单文件 ≤ 50MB、扩展名白名单（DSS §3.3.3）。
     */
    Long upload(Long contractId, MultipartFile file);

    /** 软删除附件。同一合同不能删除最后 1 个附件由前端控制（任意保留）。 */
    void delete(Long attachmentId);

    /** 生成预签名下载 URL。 */
    String previewUrl(Long attachmentId, int expireSeconds);
}
