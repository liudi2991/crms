package com.company.crms.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreatePlanDTO {
    @NotNull
    private Long contractId;

    @NotNull
    @Positive
    private Integer periodNo;

    @NotNull
    private LocalDate planDate;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal planAmount;

    private Integer remindDays;
}
