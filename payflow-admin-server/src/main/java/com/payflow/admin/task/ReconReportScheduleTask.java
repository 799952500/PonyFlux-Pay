package com.payflow.admin.task;

import com.payflow.admin.service.recon.ReconReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 对账日报/周报定时生成。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconReportScheduleTask {

    private final ReconReportService reconReportService;

    @Scheduled(cron = "0 30 8 * * ?")
    public void dailyReport() {
        try {
            int n = reconReportService.generateAndNotify("DAILY");
            log.info("对账日报生成完成 count={}", n);
        } catch (Exception e) {
            log.error("对账日报生成失败", e);
        }
    }

    @Scheduled(cron = "0 0 9 ? * MON")
    public void weeklyReport() {
        try {
            int n = reconReportService.generateAndNotify("WEEKLY");
            log.info("对账周报生成完成 count={}", n);
        } catch (Exception e) {
            log.error("对账周报生成失败", e);
        }
    }
}
