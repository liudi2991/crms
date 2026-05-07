package com.company.crms.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateParamDTO {
    @NotBlank
    private String paramKey;

    @NotBlank
    private String paramValue;
}
