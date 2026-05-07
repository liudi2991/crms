package com.company.crms.report.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TodoItemVO implements Serializable {
    /** CONTRACT_DUE / PAYMENT_DUE / PAYMENT_OVERDUE */
    private String type;
    private String title;
    private String linkUrl;
    private LocalDate date;
    private BigDecimal amount;
    private Integer overdueDays;
    private Long bizId;
}
