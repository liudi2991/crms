package com.company.crms.payment.service.impl;

import com.company.crms.common.annotation.OperationLog;
import com.company.crms.common.exception.BizException;
import com.company.crms.common.exception.ErrorCode;
import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.entity.PaymentRecord;
import com.company.crms.payment.entity.PaymentSettlement;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import com.company.crms.payment.mapper.PaymentRecordMapper;
import com.company.crms.payment.mapper.PaymentSettlementMapper;
import com.company.crms.payment.service.RedReverseService;
import com.company.crms.system.service.ChangeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 红冲实现：
 * 1) 锁定原 record，校验未被红冲；
 * 2) 反向修订与原 record 关联的所有 settlement（plan.settled_amount -= settle_amount，
 *    unsettled_amount += settle_amount，状态回退）；删除 settlement；
 * 3) 原 record 状态置为 REVERSED；
 * 4) 新增一条负数金额、status=RED 的红冲 record，引用原 id；
 * 5) 写 change_log 留痕。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedReverseServiceImpl implements RedReverseService {

    private final PaymentRecordMapper recordMapper;
    private final PaymentSettlementMapper settlementMapper;
    private final PaymentPlanMapper planMapper;
    private final ChangeLogService changeLogService;

    @Override
    @OperationLog(module = "回款", action = "红冲", type = "UPDATE")
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Long redReverse(Long originalRecordId, BigDecimal redAmount, String reason) {
        if (redAmount == null || redAmount.signum() <= 0) {
            throw new BizException(ErrorCode.PM_RED_AMOUNT_INVALID);
        }
        PaymentRecord original = recordMapper.selectForUpdate(originalRecordId);
        if (original == null) {
            throw new BizException(ErrorCode.PM_PLAN_NOT_FOUND, "原回款记录不存在");
        }
        if ("REVERSED".equals(original.getStatus()) || "RED".equals(original.getStatus())) {
            throw new BizException(ErrorCode.PM_RED_REVERSED);
        }
        if (redAmount.compareTo(original.getAmount()) > 0) {
            throw new BizException(ErrorCode.PM_RED_AMOUNT_INVALID, "红冲金额不能超过原金额");
        }

        // 1) 反向核销：把原 record 关联的 settlement 全部回滚
        List<PaymentSettlement> ss = settlementMapper.selectByRecordForUpdate(originalRecordId);
        for (PaymentSettlement s : ss) {
            PaymentPlan plan = planMapper.selectById(s.getPaymentPlanId());
            if (plan == null) continue;
            BigDecimal newSettled = plan.getSettledAmount().subtract(s.getSettleAmount()).max(BigDecimal.ZERO);
            BigDecimal newUnsettled = plan.getUnsettledAmount().add(s.getSettleAmount());
            PaymentPlan upd = new PaymentPlan();
            upd.setId(plan.getId());
            upd.setVersion(plan.getVersion());
            upd.setSettledAmount(newSettled);
            upd.setUnsettledAmount(newUnsettled);
            upd.setStatus(newSettled.signum() == 0 ? "PENDING" : "PARTIAL");
            int rows = planMapper.updateById(upd);
            if (rows == 0) {
                throw new BizException(ErrorCode.SYS_ERROR, "并发冲突，红冲失败");
            }
        }
        settlementMapper.deleteByRecord(originalRecordId);

        // 2) 原 record 置为 REVERSED + unallocated 归零
        PaymentRecord updOrig = new PaymentRecord();
        updOrig.setId(original.getId());
        updOrig.setVersion(original.getVersion());
        updOrig.setStatus("REVERSED");
        updOrig.setUnallocatedAmount(BigDecimal.ZERO);
        recordMapper.updateById(updOrig);

        // 3) 新增 RED 记录
        PaymentRecord red = new PaymentRecord();
        red.setId(SnowflakeIdGenerator.next());
        red.setContractId(original.getContractId());
        red.setArrivalDate(LocalDate.now());
        red.setAmount(redAmount.negate());
        red.setPayer(original.getPayer());
        red.setVoucherNo(original.getVoucherNo());
        red.setStatus("RED");
        red.setRedRefId(originalRecordId);
        red.setUnallocatedAmount(BigDecimal.ZERO);
        red.setRemark(reason);
        recordMapper.insert(red);

        // 4) change log
        changeLogService.record("PAYMENT_RECORD", originalRecordId, "status",
                original.getStatus(), "REVERSED", reason);
        changeLogService.record("PAYMENT_RECORD", originalRecordId, "red_amount",
                "0", redAmount.toPlainString(), reason);

        log.warn("[red] reverse record={} amount={} new={}", originalRecordId, redAmount, red.getId());
        return red.getId();
    }
}
