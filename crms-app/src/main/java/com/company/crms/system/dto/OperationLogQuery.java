package com.company.crms.system.dto;

import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogQuery {
    private String keyword;
    private Long operatorId;
    private String module;
    private String opType;
    private String bizType;
    private String result;
    private LocalDateTime fromTime;
    private LocalDateTime toTime;

    @Min(1)
    private Integer page = 1;

    @Min(1)
    private Integer size = 20;
}
