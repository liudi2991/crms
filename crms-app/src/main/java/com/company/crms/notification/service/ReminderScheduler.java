package com.company.crms.notification.service;

import com.company.crms.contract.entity.Contract;
import com.company.crms.contract.service.ContractService;
import com.company.crms.system.service.SystemParamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 到期提醒定时任务（DSS §3.5.2）：
 * <ul>
 *   <li>每日 08:00 把过到期日的合同迁到 EXPIRED；</li>
 *   <li>每日 09:00 推送合同到期提醒（默认提前 30 天）。</li>
 * </ul>
 *
 * <p>多实例下使用 ShedLock 保证只在一个节点执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ContractService contractService;
    private final SystemParamService systemParamService;
    private final NotificationDispatcher dispatcher;

    @Scheduled(cron = "0 0 8 * * ?")
    @SchedulerLock(name = "ReminderScheduler.expireContracts", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void expireContracts() {
        contractService.markExpiredAuto();
    }

    @Scheduled(cron = "0 0 9 * * ?")
    @SchedulerLock(name = "ReminderScheduler.dispatchContractDueReminder",
                   lockAtMostFor = "PT15M", lockAtLeastFor = "PT1M")
    public void dispatchContractDueReminder() {
        int advance = systemParamService.getInt("reminder.contract_due_days", 30);
        List<Contract> contracts = contractService.dueSoon(advance);
        log.info("[reminder] contract due-soon: count={} advanceDays={}", contracts.size(), advance);
        for (Contract c : contracts) {
            dispatcher.contractDue(c, advance);
        }
    }
}
