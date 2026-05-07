package com.company.crms.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RedReverseDTO {
    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal redAmount;

    @Size(max = 500)
    private String reason;
}
