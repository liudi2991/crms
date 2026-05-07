package com.company.crms.customer.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CustomerVO {
    private Long id;
    private String code;
    private String name;
    private String shortName;
    private String type;
    private String uscc;
    private String regionCode;
    private String industry;
    private String level;
    private Long ownerId;
    private String ownerName;
    private Long deptId;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
