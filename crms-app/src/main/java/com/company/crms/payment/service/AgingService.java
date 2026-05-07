package com.company.crms.payment.service;

import com.company.crms.payment.vo.AgingBucketVO;
import com.company.crms.payment.vo.AgingDrillVO;

import java.time.LocalDate;
import java.util.List;

public interface AgingService {

    /**
     * 账龄桶（DSS §3.6.2）：未到期 / 0-30 / 31-60 / 61-90 / 90+。
     *
     * <p>口径：以 {@code today} 为基准日，对每条 unsettled_amount > 0 的 PaymentPlan
     * 计算 {@code overdueDays = today - plan_date}，按桶聚合 unsettled_amount。
     */
    List<AgingBucketVO> aging(LocalDate today);

    /**
     * 账龄钻取：取某个桶下未结清的 PaymentPlan 列表（带合同信息）。
     */
    List<AgingDrillVO> drill(LocalDate today, String bucket, int page, int size);
}
