package com.company.crms.payment;

import com.company.crms.common.exception.BizException;
import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.entity.PaymentRecord;
import com.company.crms.payment.entity.PaymentSettlement;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import com.company.crms.payment.mapper.PaymentRecordMapper;
import com.company.crms.payment.mapper.PaymentSettlementMapper;
import com.company.crms.payment.service.impl.RedReverseServiceImpl;
import com.company.crms.system.service.ChangeLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 红冲流程测试：原 record→REVERSED；plan 反向回滚；产生 status=RED 的负数 record。
 */
@ExtendWith(MockitoExtension.class)
class RedReverseTest {

    @Mock PaymentRecordMapper recordMapper;
    @Mock PaymentPlanMapper planMapper;
    @Mock PaymentSettlementMapper settlementMapper;
    @Mock ChangeLogService changeLogService;

    @InjectMocks RedReverseServiceImpl service;

    @Test
    void redReverse_full_amount() {
        PaymentRecord original = new PaymentRecord();
        original.setId(1L);
        original.setContractId(99L);
        original.setAmount(new BigDecimal("1000.00"));
        original.setStatus("NORMAL");
        when(recordMapper.selectForUpdate(1L)).thenReturn(original);

        PaymentSettlement s = new PaymentSettlement();
        s.setId(900L);
        s.setPaymentPlanId(10L);
        s.setPaymentRecordId(1L);
        s.setSettleAmount(new BigDecimal("600.00"));
        when(settlementMapper.selectByRecordForUpdate(1L)).thenReturn(List.of(s));

        PaymentPlan plan = new PaymentPlan();
        plan.setId(10L);
        plan.setSettledAmount(new BigDecimal("600.00"));
        plan.setUnsettledAmount(new BigDecimal("400.00"));
        plan.setVersion(0);
        when(planMapper.selectById(10L)).thenReturn(plan);
        when(planMapper.updateById(any())).thenReturn(1);

        Long redId = service.redReverse(1L, new BigDecimal("1000.00"), "客户取消");

        assertNotNull(redId);
        // 原 record 状态置为 REVERSED
        ArgumentCaptor<PaymentRecord> recCap = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(recordMapper, atLeastOnce()).updateById(recCap.capture());
        boolean hasReversed = recCap.getAllValues().stream().anyMatch(r -> "REVERSED".equals(r.getStatus()));
        assert hasReversed;

        // settlement 删除
        verify(settlementMapper).deleteByRecord(1L);
        // plan 反向：settled 600 -> 0；unsettled 400 -> 1000；status 回到 PENDING
        ArgumentCaptor<PaymentPlan> pCap = ArgumentCaptor.forClass(PaymentPlan.class);
        verify(planMapper).updateById(pCap.capture());
        assertEquals(0, pCap.getValue().getSettledAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, pCap.getValue().getUnsettledAmount().compareTo(new BigDecimal("1000.00")));
        assertEquals("PENDING", pCap.getValue().getStatus());

        // 产生新的 RED 记录
        ArgumentCaptor<PaymentRecord> insertCap = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(recordMapper).insert(insertCap.capture());
        PaymentRecord red = insertCap.getValue();
        assertEquals("RED", red.getStatus());
        assertEquals(new BigDecimal("-1000.00"), red.getAmount());
        assertEquals(1L, red.getRedRefId());
    }

    @Test
    void redReverse_rejects_already_reversed() {
        PaymentRecord original = new PaymentRecord();
        original.setId(1L);
        original.setContractId(99L);
        original.setAmount(new BigDecimal("100.00"));
        original.setStatus("REVERSED");
        when(recordMapper.selectForUpdate(1L)).thenReturn(original);

        assertThrows(BizException.class,
                () -> service.redReverse(1L, new BigDecimal("100.00"), "再红冲"));
    }

    @Test
    void redReverse_rejects_amount_exceeds_original() {
        PaymentRecord original = new PaymentRecord();
        original.setId(1L);
        original.setContractId(99L);
        original.setAmount(new BigDecimal("100.00"));
        original.setStatus("NORMAL");
        when(recordMapper.selectForUpdate(1L)).thenReturn(original);

        assertThrows(BizException.class,
                () -> service.redReverse(1L, new BigDecimal("999.00"), "金额过大"));
    }
}
