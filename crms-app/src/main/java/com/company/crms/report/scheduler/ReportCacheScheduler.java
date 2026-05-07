package com.company.crms.report.scheduler;

import com.company.crms.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 看板缓存预热（DSS §3.6.4）：
 * <ul>
 *   <li>每 30 分钟失效一次（保证数据时效），并立即预热高频报表；</li>
 *   <li>每日 02:00 重新预热一次（覆盖凌晨结算后数据）。</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportCacheScheduler {

    private final ReportService reportService;

    @Scheduled(fixedDelayString = "PT30M", initialDelayString = "PT2M")
    @SchedulerLock(name = "ReportCacheScheduler.refresh", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void refresh() {
        reportService.evictCache();
        reportService.warmupCache();
    }

    @Scheduled(cron = "0 0 2 * * ?")
    @SchedulerLock(name = "ReportCacheScheduler.daily", lockAtMostFor = "PT10M", lockAtLeastFor = "PT1M")
    public void daily() {
        reportService.evictCache();
        reportService.warmupCache();
    }
}
