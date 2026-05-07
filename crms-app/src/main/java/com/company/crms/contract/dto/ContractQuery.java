package com.company.crms.contract.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
public class ContractQuery {
    private String keyword;
    private String type;
    private String status;
    private Long customerId;
    private Long ownerId;
    private LocalDate signedFrom;
    private LocalDate signedTo;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    private Integer size = 20;
}
