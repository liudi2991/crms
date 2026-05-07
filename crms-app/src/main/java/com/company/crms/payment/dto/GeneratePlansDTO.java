package com.company.crms.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

/**
 * 根据合同自动生成回款计划。
 * 频率：MONTHLY / QUARTERLY / ONCE。
 */
@Data
public class GeneratePlansDTO {
    @NotNull
    private Long contractId;

    @NotNull
    private LocalDate firstPlanDate;

    @NotNull
    @Min(1)
    private Integer periods;

    @NotNull
    @Pattern(regexp = "MONTHLY|QUARTERLY|ONCE")
    private String frequency;

    /** 是否覆盖现有计划（删除后重建）。 */
    private Boolean overwrite = false;
}
