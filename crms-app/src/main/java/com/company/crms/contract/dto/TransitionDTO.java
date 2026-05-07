package com.company.crms.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TransitionDTO {
    @NotBlank
    private String to;

    @Size(max = 255)
    private String reason;
}
