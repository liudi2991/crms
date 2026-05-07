package com.company.crms.payment.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ManualSettleDTO {
    @NotNull
    private Long recordId;

    @NotEmpty
    private List<Long> planIds;
}
