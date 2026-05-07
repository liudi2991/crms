package com.company.crms.payment;

import com.company.crms.common.exception.BizException;
import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.entity.PaymentRecord;
import com.company.crms.payment.entity.PaymentSettlement;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import com.company.crms.payment.mapper.PaymentRecordMapper;
import com.company.crms.payment.mapper.PaymentSettlementMapper;
import com.company.crms.payment.service.impl.SettlementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 自动核销算法单测（DSS §3.4.1）。
 * 用例覆盖：金额刚好抵扣、跨多期、剩余 unallocated、被红冲拒绝。
 */
@ExtendWith(MockitoExtension.class)
class SettlementAlgorithmTest {

    @Mock PaymentPlanMapper planMapper;
    @Mock PaymentRecordMapper recordMapper;
    @Mock PaymentSettlementMapper settlementMapper;

    @InjectMocks SettlementServiceImpl service;

    private PaymentRecord record;

    @BeforeEach
    void setUp() {
        record = new PaymentRecord();
        record.setId(100L);
        record.setContractId(1L);
        record.setAmount(new BigDecimal("1500.00"));
        record.setUnallocatedAmount(new BigDecimal("1500.00"));
        record.setStatus("NORMAL");
    }

    private PaymentPlan plan(long id, String unsettled) {
        PaymentPlan p = new PaymentPlan();
        p.setId(id);
        p.setContractId(1L);
        p.setPlanAmount(new BigDecimal(unsettled));
        p.setSettledAmount(BigDecimal.ZERO);
        p.setUnsettledAmount(new BigDecimal(unsettled));
        p.setStatus("PENDING");
        p.setPlanDate(LocalDate.of(2026, 1, 1));
        p.setVersion(0);
        return p;
    }

    @Test
    void should_settle_first_plan_full_then_partial_second() {
        when(recordMapper.selectForUpdate(100L)).thenReturn(record);
        List<PaymentPlan> plans = new ArrayList<>(List.of(
                plan(10L, "1000.00"),
                plan(11L, "800.00"),
                plan(12L, "500.00")
        ));
        when(planMapper.selectUnsettledForUpdate(1L)).thenReturn(plans);
        when(planMapper.updateById(any())).thenReturn(1);

        BigDecimal settled = service.settle(record, null);

        assertEquals(new BigDecimal("1500.00"), settled);

        ArgumentCaptor<PaymentSettlement> captor = ArgumentCaptor.forClass(PaymentSettlement.class);
        verify(settlementMapper, times(2)).insert(captor.capture());
        List<PaymentSettlement> calls = captor.getAllValues();
        assertEquals(new BigDecimal("1000.00"), calls.get(0).getSettleAmount());
        assertEquals(10L, calls.get(0).getPaymentPlanId());
        assertEquals(new BigDecimal("500.00"), calls.get(1).getSettleAmount());
        assertEquals(11L, calls.get(1).getPaymentPlanId());

        ArgumentCaptor<PaymentRecord> recCap = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(recordMapper).updateById(recCap.capture());
        assertEquals(BigDecimal.ZERO.setScale(2), recCap.getValue().getUnallocatedAmount().setScale(2));
    }

    @Test
    void should_leave_unallocated_when_record_exceeds_total_plans() {
        record.setAmount(new BigDecimal("3000.00"));
        record.setUnallocatedAmount(new BigDecimal("3000.00"));
        when(recordMapper.selectForUpdate(100L)).thenReturn(record);
        when(planMapper.selectUnsettledForUpdate(1L)).thenReturn(new ArrayList<>(List.of(
                plan(10L, "1000.00"),
                plan(11L, "800.00")
        )));
        when(planMapper.updateById(any())).thenReturn(1);

        BigDecimal settled = service.settle(record, null);
        assertEquals(new BigDecimal("1800.00"), settled);

        ArgumentCaptor<PaymentRecord> recCap = ArgumentCaptor.forClass(PaymentRecord.class);
        verify(recordMapper).updateById(recCap.capture());
        assertEquals(new BigDecimal("1200.00"), recCap.getValue().getUnallocatedAmount());
    }

    @Test
    void manual_settle_uses_target_plan_ids_only() {
        when(recordMapper.selectForUpdate(100L)).thenReturn(record);
        when(planMapper.selectByIdsForUpdate(List.of(11L)))
                .thenReturn(new ArrayList<>(List.of(plan(11L, "2000.00"))));
        when(planMapper.updateById(any())).thenReturn(1);

        service.settle(record, List.of(11L));

        verify(planMapper).selectByIdsForUpdate(List.of(11L));
        verify(planMapper, times(0)).selectUnsettledForUpdate(eq(1L));
        verify(settlementMapper, atLeastOnce()).insert(any());
    }

    @Test
    void should_reject_reversed_record() {
        record.setStatus("REVERSED");
        when(recordMapper.selectForUpdate(100L)).thenReturn(record);
        assertThrows(BizException.class, () -> service.settle(record, null));
    }
}
