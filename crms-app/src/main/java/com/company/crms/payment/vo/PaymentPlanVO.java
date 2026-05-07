package com.company.crms.payment.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentPlanVO implements Serializable {
    private Long id;
    private Long contractId;
    private String contractCode;
    private String contractName;
    private Integer periodNo;
    private LocalDate planDate;
    private BigDecimal planAmount;
    private BigDecimal settledAmount;
    private BigDecimal unsettledAmount;
    private String status;
    private Boolean overdue;
    private Integer overdueDays;
    private Integer remindDays;
    private Integer version;
}
