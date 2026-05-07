package com.company.crms.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecycleBinItemVO {
    private String bizType;
    private Long id;
    private String code;
    private String name;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
