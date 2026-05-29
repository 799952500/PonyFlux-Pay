package com.payflow.admin.task;

import com.payflow.admin.service.recon.ReconSlaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SLA 扫描：due-soon 提醒与 overdue 自动升级。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconSlaScanTask {

    private final ReconSlaService reconSlaService;

    @Scheduled(cron = "0 */1 * * * ?")
    public void scanSla() {
        try {
            int[] counts = reconSlaService.scanDueSoonAndOverdue();
            if (counts[0] > 0 || counts[1] > 0) {
                log.info("SLA 扫描完成: dueSoon={}, overdueEscalated={}", counts[0], counts[1]);
            }
        } catch (Exception e) {
            log.error("SLA 扫描失败", e);
        }
    }
}
