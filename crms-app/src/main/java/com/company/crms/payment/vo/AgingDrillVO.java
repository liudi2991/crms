package com.company.crms.payment.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AgingDrillVO implements Serializable {
    private Long planId;
    private Long contractId;
    private String contractCode;
    private String contractName;
    private Integer periodNo;
    private LocalDate planDate;
    private BigDecimal unsettledAmount;
    private Integer overdueDays;
}
