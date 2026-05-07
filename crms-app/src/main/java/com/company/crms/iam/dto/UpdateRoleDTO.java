package com.company.crms.iam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class UpdateRoleDTO {

    @NotBlank
    @Size(max = 64)
    private String name;

    @NotBlank
    @Pattern(regexp = "ALL|DEPT|SELF", message = "数据范围必须为 ALL/DEPT/SELF")
    private String dataScope;

    @Size(max = 255)
    private String description;

    private Integer sort;

    @NotNull
    private List<String> permissionCodes;

    @NotNull
    private Integer version;
}
