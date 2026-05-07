package com.company.crms.system.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RecycleBinQuery {
    @NotBlank
    private String bizType;

    private String keyword;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    private Integer size = 20;
}
