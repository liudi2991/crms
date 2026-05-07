package com.company.crms.contract.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ContractAttachmentVO implements Serializable {
    private Long id;
    private Long contractId;
    private Long fileObjectId;
    private String fileName;
    private Long fileSize;
    private Long uploadedBy;
    private LocalDateTime uploadedAt;
    private String previewUrl;
}
