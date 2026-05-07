package com.company.crms.payment.service.impl;

import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import com.company.crms.payment.service.AgingService;
import com.company.crms.payment.vo.AgingBucketVO;
import com.company.crms.payment.vo.AgingDrillVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgingServiceImpl implements AgingService {

    private final PaymentPlanMapper planMapper;

    @Override
    public List<AgingBucketVO> aging(LocalDate today) {
        QueryWrapper<PaymentPlan> w = new QueryWrapper<>();
        w.gt("unsettled_amount", 0).eq("is_deleted", 0).ne("status", "SETTLED");
        List<PaymentPlan> plans = planMapper.selectList(w);

        BigDecimal[] amounts = new BigDecimal[5];
        long[] counts = new long[5];
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = BigDecimal.ZERO;
        }
        for (PaymentPlan p : plans) {
            int idx = bucketIndex(today, p.getPlanDate());
            amounts[idx] = amounts[idx].add(p.getUnsettledAmount());
            counts[idx] += 1;
        }
        return List.of(
                new AgingBucketVO("UNDUE",  amounts[0], counts[0]),
                new AgingBucketVO("0-30",   amounts[1], counts[1]),
                new AgingBucketVO("31-60",  amounts[2], counts[2]),
                new AgingBucketVO("61-90",  amounts[3], counts[3]),
                new AgingBucketVO("90+",    amounts[4], counts[4])
        );
    }

    @Override
    public List<AgingDrillVO> drill(LocalDate today, String bucket, int page, int size) {
        int p = Math.max(1, page);
        int s = Math.max(1, Math.min(size, 200));
        return planMapper.drillBucket(today, bucket, (p - 1) * s, s);
    }

    /**
     * 0=UNDUE, 1=0-30, 2=31-60, 3=61-90, 4=90+
     */
    public static int bucketIndex(LocalDate today, LocalDate planDate) {
        if (today == null || planDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(planDate, today);
        if (days <= 0) return 0;
        if (days <= 30) return 1;
        if (days <= 60) return 2;
        if (days <= 90) return 3;
        return 4;
    }
}
