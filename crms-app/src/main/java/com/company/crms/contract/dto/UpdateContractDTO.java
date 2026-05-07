package com.company.crms.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateContractDTO {
    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Pattern(regexp = "SALES|PROCUREMENT|SERVICE|OTHER")
    private String type;

    @NotNull
    private Long customerId;

    @NotNull
    @DecimalMin(value = "0.01", message = "合同金额必须 > 0")
    private BigDecimal amount;

    @NotNull
    private LocalDate signedAt;

    @NotNull
    private LocalDate performStartAt;

    @NotNull
    private LocalDate performEndAt;

    private Integer remindDays;

    private Long ownerId;

    @Size(max = 1000)
    private String remark;

    @NotNull
    private Integer version;
}
