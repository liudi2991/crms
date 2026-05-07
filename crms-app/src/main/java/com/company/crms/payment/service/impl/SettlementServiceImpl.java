package com.company.crms.payment.service.impl;

import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.entity.PaymentRecord;
import com.company.crms.payment.entity.PaymentSettlement;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import com.company.crms.payment.mapper.PaymentRecordMapper;
import com.company.crms.payment.mapper.PaymentSettlementMapper;
import com.company.crms.payment.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实现自动核销算法。事务在本类入口开启，内部 SQL 全部走 FOR UPDATE 行锁。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final PaymentPlanMapper planMapper;
    private final PaymentRecordMapper recordMapper;
    private final PaymentSettlementMapper settlementMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public BigDecimal settle(PaymentRecord record, List<Long> targetPlanIds) {
        // 1) 锁定 record，避免并发重复核销
        PaymentRecord locked = recordMapper.selectForUpdate(record.getId());
        if (locked == null) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND, "回款记录不存在");
        }
        if ("REVERSED".equals(locked.getStatus()) || "RED".equals(locked.getStatus())) {
            throw new BizException(ErrorCode.PM_RED_REVERSED);
        }

        // 2) 取候选计划
        List<PaymentPlan> plans = (targetPlanIds == null || targetPlanIds.isEmpty())
                ? planMapper.selectUnsettledForUpdate(locked.getContractId())
                : planMapper.selectByIdsForUpdate(targetPlanIds);
        plans.removeIf(p -> "SETTLED".equals(p.getStatus()));

        // 3) 计算可核销余额
        BigDecimal remaining = locked.getUnallocatedAmount() == null
                ? locked.getAmount()
                : locked.getUnallocatedAmount();
        BigDecimal totalSettled = BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();

        for (PaymentPlan plan : plans) {
            if (remaining.signum() <= 0) {
                break;
            }
            BigDecimal portion = plan.getUnsettledAmount().min(remaining);
            if (portion.signum() <= 0) {
                continue;
            }

            // 写 settlement
            PaymentSettlement st = new PaymentSettlement();
            st.setId(SnowflakeIdGenerator.next());
            st.setPaymentPlanId(plan.getId());
            st.setPaymentRecordId(locked.getId());
            st.setSettleAmount(portion);
            st.setSettleAt(now);
            st.setSettleType(targetPlanIds == null ? "AUTO" : "MANUAL");
            settlementMapper.insert(st);

            // 更新 plan
            BigDecimal newSettled = plan.getSettledAmount().add(portion);
            BigDecimal newUnsettled = plan.getUnsettledAmount().subtract(portion);
            PaymentPlan upd = new PaymentPlan();
            upd.setId(plan.getId());
            upd.setVersion(plan.getVersion());
            upd.setSettledAmount(newSettled);
            upd.setUnsettledAmount(newUnsettled);
            upd.setStatus(newUnsettled.signum() == 0 ? "SETTLED" : "PARTIAL");
            int rows = planMapper.updateById(upd);
            if (rows == 0) {
                throw new BizException(ErrorCode.SYS_ERROR, "并发冲突，核销失败");
            }

            remaining = remaining.subtract(portion);
            totalSettled = totalSettled.add(portion);
        }

        // 4) 回写 record 的 unallocated_amount
        PaymentRecord updRec = new PaymentRecord();
        updRec.setId(locked.getId());
        updRec.setVersion(locked.getVersion());
        updRec.setUnallocatedAmount(remaining);
        recordMapper.updateById(updRec);

        log.info("[settle] record={} contract={} settled={} remaining={}",
                locked.getId(), locked.getContractId(), totalSettled, remaining);
        return totalSettled;
    }
}
