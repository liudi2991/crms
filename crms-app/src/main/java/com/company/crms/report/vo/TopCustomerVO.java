package com.company.crms.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopCustomerVO {
    private Long customerId;
    private String customerName;
    private BigDecimal amount;
}
