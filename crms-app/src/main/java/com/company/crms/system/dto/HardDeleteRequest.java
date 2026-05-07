package com.company.crms.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class HardDeleteRequest {
    @NotBlank
    @Size(max = 500)
    private String reason;
}
