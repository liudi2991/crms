package com.company.crms.iam.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class AssignRolesDTO {

    @NotNull
    @Size(min = 1, message = "至少分配一个角色")
    private List<Long> roleIds;
}
