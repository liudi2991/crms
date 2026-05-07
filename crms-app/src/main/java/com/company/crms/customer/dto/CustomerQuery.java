package com.company.crms.customer.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CustomerQuery {
    private String keyword;
    private String type;
    private String level;
    private String status;
    private Long ownerId;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    private Integer size = 20;
}
