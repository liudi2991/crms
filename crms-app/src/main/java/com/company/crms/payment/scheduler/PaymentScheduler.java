package com.company.crms.payment.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.crms.notification.service.NotificationDispatcher;
import com.company.crms.payment.entity.PaymentPlan;
import com.company.crms.payment.mapper.PaymentPlanMapper;
import com.company.crms.payment.service.PaymentPlanService;
import com.company.crms.system.service.SystemParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 回款相关定时任务（DSS §3.5.2）：
 * <ul>
 *   <li>每日 06:00 标记逾期：plan_date &lt; today 且未结清 -> is_overdue=1</li>
 *   <li>每日 09:30 临期提醒：默认提前 7 天</li>
 *   <li>每日 09:45 逾期提醒</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentScheduler {

    private final PaymentPlanService planService;
    private final PaymentPlanMapper planMapper;
    private final SystemParamService systemParamService;
    private final NotificationDispatcher dispatcher;

    @Scheduled(cron = "0 0 6 * * ?")
    @SchedulerLock(name = "PaymentScheduler.markOverdue", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void markOverdue() {
        planService.markOverdueAuto();
    }

    @Scheduled(cron = "0 30 9 * * ?")
    @SchedulerLock(name = "PaymentScheduler.dueSoonReminder", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    public void dueSoonReminder() {
        int advance = systemParamService.getInt("reminder.payment_due_days", 7);
        LocalDate today = LocalDate.now();
        QueryWrapper<PaymentPlan> w = new QueryWrapper<PaymentPlan>()
                .gt("unsettled_amount", 0)
                .ne("status", "SETTLED")
                .between("plan_date", today, today.plusDays(advance));
        List<PaymentPlan> due = planMapper.selectList(w);
        log.info("[reminder] payment due-soon advance={}d count={}", advance, due.size());
        for (PaymentPlan p : due) {
            dispatcher.paymentPlanDue(p.getContractId(), p.getId(), advance);
        }
    }

    @Scheduled(cron = "0 45 9 * * ?")
    @SchedulerLock(name = "PaymentScheduler.overdueReminder", lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    public void overdueReminder() {
        LocalDate today = LocalDate.now();
        QueryWrapper<PaymentPlan> w = new QueryWrapper<PaymentPlan>()
                .gt("unsettled_amount", 0)
                .eq("is_overdue", 1)
                .ne("status", "SETTLED");
        List<PaymentPlan> overdue = planMapper.selectList(w);
        log.info("[reminder] payment overdue count={}", overdue.size());
        for (PaymentPlan p : overdue) {
            int days = (int) java.time.temporal.ChronoUnit.DAYS.between(p.getPlanDate(), today);
            dispatcher.paymentPlanOverdue(p.getContractId(), p.getId(), days);
        }
    }
}
