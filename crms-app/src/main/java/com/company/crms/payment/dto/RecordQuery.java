package com.company.crms.payment.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RecordQuery {
    private Long contractId;
    private String keyword;
    private String status;
    private LocalDate fromDate;
    private LocalDate toDate;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    private Integer size = 20;
}
