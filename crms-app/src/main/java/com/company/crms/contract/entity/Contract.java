package com.company.crms.contract.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.crms.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {
    private String code;
    private String name;
    private String type;
    private Long customerId;
    private BigDecimal amount;
    private LocalDate signedAt;
    private LocalDate performStartAt;
    private LocalDate performEndAt;
    private Integer remindDays;
    private Long ownerId;
    private Long deptId;
    private String status;
    private String remark;
}
