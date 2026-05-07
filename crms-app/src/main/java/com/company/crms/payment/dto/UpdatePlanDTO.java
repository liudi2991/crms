package com.company.crms.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdatePlanDTO {
    @NotNull
    private LocalDate planDate;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal planAmount;

    private Integer remindDays;

    @NotNull
    private Integer version;
}
