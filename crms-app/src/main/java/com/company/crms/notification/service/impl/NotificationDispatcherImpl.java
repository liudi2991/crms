package com.company.crms.notification.service.impl;

import com.company.crms.common.util.SnowflakeIdGenerator;
import com.company.crms.contract.entity.Contract;
import com.company.crms.contract.mapper.ContractMapper;
import com.company.crms.notification.entity.Notification;
import com.company.crms.notification.entity.NotificationSetting;
import com.company.crms.notification.mapper.NotificationMapper;
import com.company.crms.notification.mapper.NotificationSettingMapper;
import com.company.crms.notification.service.NotificationDispatcher;
import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 通知分发：
 * <ul>
 *   <li>偏好检查（user_id + scene），未配置默认开启；</li>
 *   <li>去重：24h 内同 receiver+scene+biz_id 不重复发；</li>
 *   <li>落库 notification 表，邮件/IM 暂留 stub 日志。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatcherImpl implements NotificationDispatcher {

    private static final long DEDUP_HOURS = 24L;

    private final NotificationMapper notificationMapper;
    private final NotificationSettingMapper settingMapper;
    private final ContractMapper contractMapper;
    private final PaymentPlanMapper planMapper;

    @Override
    @Transactional
    public void contractDue(Contract contract, int advanceDays) {
        if (contract.getOwnerId() == null) return;
        String title = String.format("合同 %s 即将到期（%d 天）", contract.getCode(), advanceDays);
        String content = String.format("合同 [%s] %s 履约结束日期 %s，请及时跟进续约或验收。",
                contract.getCode(), contract.getName(), contract.getPerformEndAt());
        send(contract.getOwnerId(), "CONTRACT_DUE", "CONTRACT", contract.getId(),
                title, content, "/contracts/" + contract.getId());
    }

    @Override
    @Transactional
    public void paymentPlanDue(Long contractId, Long planId, int advanceDays) {
        Contract c = contractMapper.selectById(contractId);
        PaymentPlan p = planMapper.selectById(planId);
        if (c == null || p == null || c.getOwnerId() == null) return;
        String title = String.format("回款计划 %s #%d 临期", c.getCode(), p.getPeriodNo());
        String content = String.format("合同 %s 第 %d 期回款 %s，金额 %s，未结清 %s。",
                c.getCode(), p.getPeriodNo(), p.getPlanDate(), p.getPlanAmount(), p.getUnsettledAmount());
        send(c.getOwnerId(), "PAYMENT_DUE", "PAYMENT_PLAN", planId, title, content,
                "/contracts/" + contractId);
    }

    @Override
    @Transactional
    public void paymentPlanOverdue(Long contractId, Long planId, int overdueDays) {
        Contract c = contractMapper.selectById(contractId);
        PaymentPlan p = planMapper.selectById(planId);
        if (c == null || p == null || c.getOwnerId() == null) return;
        String title = String.format("回款逾期 %d 天：%s #%d", overdueDays, c.getCode(), p.getPeriodNo());
        String content = String.format("合同 %s 第 %d 期回款已逾期 %d 天，未结清 %s。",
                c.getCode(), p.getPeriodNo(), overdueDays, p.getUnsettledAmount());
        send(c.getOwnerId(), "PAYMENT_OVERDUE", "PAYMENT_PLAN", planId, title, content,
                "/contracts/" + contractId);
    }

    private void send(Long receiverId, String scene, String bizType, Long bizId,
                      String title, String content, String linkUrl) {
        NotificationSetting setting = settingMapper.findByUserScene(receiverId, scene);
        if (setting != null && setting.getEnabled() != null && setting.getEnabled() == 0) {
            log.debug("[notify] skip: user {} disabled scene {}", receiverId, scene);
            return;
        }
        long dup = notificationMapper.countDuplicate(
                receiverId, scene, bizId, LocalDateTime.now().minusHours(DEDUP_HOURS));
        if (dup > 0) {
            log.debug("[notify] skip dup: user={} scene={} biz={}", receiverId, scene, bizId);
            return;
        }
        Notification n = new Notification();
        n.setId(SnowflakeIdGenerator.next());
        n.setReceiverId(receiverId);
        n.setScene(scene);
        n.setBizType(bizType);
        n.setBizId(bizId);
        n.setTitle(title.length() > 128 ? title.substring(0, 128) : title);
        n.setContent(content.length() > 500 ? content.substring(0, 500) : content);
        n.setLinkUrl(linkUrl);
        n.setIsRead(0);
        n.setArchived(0);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
        log.info("[notify] saved id={} scene={} receiver={} biz={}", n.getId(), scene, receiverId, bizId);
    }
}
