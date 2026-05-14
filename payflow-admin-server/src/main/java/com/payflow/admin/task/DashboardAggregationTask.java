package com.payflow.admin.task;

import com.payflow.admin.service.DashboardAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 仪表盘数据聚合定时任务。
 * 三级粒度：每5分钟增量、每小时汇总、每日汇总。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardAggregationTask {

    private final DashboardAggregationService dashboardAggregationService;

    /**
     * 每5分钟增量聚合（T+0），聚合最近10分钟窗口以容错。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void aggregate5min() {
        LocalDateTime end = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime start = end.minusMinutes(10);
        log.info("开始5分钟粒度聚合: {} ~ {}", start, end);
        try {
            int count = dashboardAggregationService.aggregateMetrics("5min", start, end);
            log.info("5分钟聚合完成: 写入{}条", count);
        } catch (Exception e) {
            log.error("5分钟聚合失败", e);
        }
    }

    /**
     * 每小时汇总（整点过5分执行），覆盖最近2小时窗口。
     */
    @Scheduled(cron = "0 5 * * * ?")
    public void aggregateHourly() {
        LocalDateTime end = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        LocalDateTime start = end.minusHours(2);
        log.info("开始小时粒度聚合: {} ~ {}", start, end);
        try {
            int count = dashboardAggregationService.aggregateMetrics("hour", start, end);
            log.info("小时聚合完成: 写入{}条", count);
        } catch (Exception e) {
            log.error("小时聚合失败", e);
        }
    }

    /**
     * 每日汇总（凌晨0:10执行），覆盖昨日全天 + 今日容错窗口。
     */
    @Scheduled(cron = "0 10 0 * * ?")
    public void aggregateDaily() {
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime start = today.minusDays(1);
        log.info("开始日粒度聚合: {} ~ {}", start, today);
        try {
            int count = dashboardAggregationService.aggregateMetrics("day", start, today);
            log.info("日聚合完成: 写入{}条", count);
        } catch (Exception e) {
            log.error("日聚合失败", e);
        }
    }
}
