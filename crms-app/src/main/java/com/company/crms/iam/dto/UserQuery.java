package com.company.crms.iam.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class UserQuery {
    private String keyword;
    private Long deptId;
    private String status;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    private Integer size = 20;
}
