package com.company.crms.contract.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class ContractVO implements Serializable {
    private Long id;
    private String code;
    private String name;
    private String type;
    private Long customerId;
    private String customerName;
    private BigDecimal amount;
    private LocalDate signedAt;
    private LocalDate performStartAt;
    private LocalDate performEndAt;
    private Integer remindDays;
    private Long ownerId;
    private String ownerName;
    private Long deptId;
    private String status;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer version;
}
