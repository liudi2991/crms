package com.company.crms.notification;

import com.company.crms.contract.entity.Contract;
import com.company.crms.contract.mapper.ContractMapper;
import com.company.crms.notification.entity.Notification;
import com.company.crms.notification.entity.NotificationSetting;
import com.company.crms.notification.mapper.NotificationMapper;
import com.company.crms.notification.mapper.NotificationSettingMapper;
import com.company.crms.notification.service.impl.NotificationDispatcherImpl;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 通知去重 + 偏好测试：
 * <ul>
 *   <li>同一 receiver+scene+biz 在 24h 内只发一次；</li>
 *   <li>用户关闭某 scene 时不发；</li>
 *   <li>正常路径写入 notification 表。</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class NotificationDedupTest {

    @Mock NotificationMapper notificationMapper;
    @Mock NotificationSettingMapper settingMapper;
    @Mock ContractMapper contractMapper;
    @Mock PaymentPlanMapper planMapper;

    @InjectMocks NotificationDispatcherImpl dispatcher;

    private Contract sampleContract() {
        Contract c = new Contract();
        c.setId(1L);
        c.setOwnerId(99L);
        c.setCode("CT-2026-0001");
        c.setName("测试合同");
        c.setPerformEndAt(LocalDate.of(2026, 6, 1));
        c.setAmount(new BigDecimal("100000"));
        return c;
    }

    @Test
    void contract_due_writes_notification_when_no_dup_and_enabled() {
        when(settingMapper.findByUserScene(99L, "CONTRACT_DUE")).thenReturn(null);
        when(notificationMapper.countDuplicate(eq(99L), eq("CONTRACT_DUE"), eq(1L), any(LocalDateTime.class)))
                .thenReturn(0L);

        dispatcher.contractDue(sampleContract(), 30);

        ArgumentCaptor<Notification> cap = ArgumentCaptor.forClass(Notification.class);
        verify(notificationMapper).insert(cap.capture());
        Notification n = cap.getValue();
        assertEquals(99L, n.getReceiverId());
        assertEquals("CONTRACT_DUE", n.getScene());
        assertEquals(0, n.getIsRead());
        assertEquals(0, n.getArchived());
    }

    @Test
    void contract_due_skipped_when_duplicate_exists() {
        when(settingMapper.findByUserScene(99L, "CONTRACT_DUE")).thenReturn(null);
        when(notificationMapper.countDuplicate(eq(99L), eq("CONTRACT_DUE"), eq(1L), any(LocalDateTime.class)))
                .thenReturn(1L);

        dispatcher.contractDue(sampleContract(), 30);

        verify(notificationMapper, never()).insert(any());
    }

    @Test
    void contract_due_skipped_when_user_disabled_scene() {
        NotificationSetting s = new NotificationSetting();
        s.setEnabled(0);
        when(settingMapper.findByUserScene(99L, "CONTRACT_DUE")).thenReturn(s);

        dispatcher.contractDue(sampleContract(), 30);

        verify(notificationMapper, never()).countDuplicate(anyLong(), anyString(), anyLong(), any());
        verify(notificationMapper, never()).insert(any());
    }

    @Test
    void contract_due_skipped_when_no_owner() {
        Contract c = sampleContract();
        c.setOwnerId(null);
        dispatcher.contractDue(c, 30);
        verify(notificationMapper, times(0)).insert(any());
    }
}
