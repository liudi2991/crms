package com.company.crms.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDepartmentDTO {

    @NotNull
    private Long parentId;

    @NotBlank
    @Size(max = 64)
    private String name;

    private Integer sort;

    @NotNull
    private Integer version;
}
