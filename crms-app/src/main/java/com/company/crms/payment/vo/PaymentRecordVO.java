package com.company.crms.payment.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PaymentRecordVO implements Serializable {
    private Long id;
    private Long contractId;
    private String contractCode;
    private String contractName;
    private LocalDate arrivalDate;
    private BigDecimal amount;
    private String payer;
    private String voucherNo;
    private String status;
    private Long redRefId;
    private BigDecimal unallocatedAmount;
    private String remark;
    private Long voucherFileId;
    private Integer version;
}
