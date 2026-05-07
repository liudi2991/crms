package com.company.crms.payment.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.crms.common.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payment_plan")
public class PaymentPlan extends BaseEntity {
    private Long contractId;
    private Integer periodNo;
    private LocalDate planDate;
    private BigDecimal planAmount;
    private BigDecimal settledAmount;
    private BigDecimal unsettledAmount;
    private String status;
    private Integer isOverdue;
    private Integer overdueDays;
    private Integer remindDays;
}
