package com.company.crms.payment.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgingBucketVO {
    /** 桶名：UNDUE / 0-30 / 31-60 / 61-90 / 90+ */
    private String bucket;
    /** 该桶内未结清金额合计。 */
    private BigDecimal amount;
    /** 涉及的回款计划条数。 */
    private long count;
}
