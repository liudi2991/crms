package com.company.crms.customer.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MergeCustomerDTO {

    @NotNull
    private Long mainId;

    @NotEmpty
    private List<Long> mergedIds;

    @Size(max = 500)
    private String reason;
}
