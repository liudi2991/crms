package com.company.crms.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CreateRecordDTO {
    @NotNull
    private Long contractId;

    @NotNull
    private LocalDate arrivalDate;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @Size(max = 100)
    private String payer;

    @Size(max = 255)
    private String voucherNo;

    @Size(max = 500)
    private String remark;

    private Long voucherFileId;

    /** 可选：手工指定要核销的回款计划 id 列表，留空则按 plan_date 自动核销。 */
    private List<Long> targetPlanIds;
}
