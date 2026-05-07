package com.company.crms.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrendPointVO {
    /** yyyy-MM */
    private String month;
    private BigDecimal contractAmount;
    private BigDecimal paidAmount;
}
