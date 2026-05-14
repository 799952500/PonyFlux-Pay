package com.payflow.admin.task;

import com.payflow.admin.service.ChurnAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 流失预警检测定时任务（每日凌晨2:00执行）。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChurnDetectionTask {

    private final ChurnAlertService churnAlertService;

    @Scheduled(cron = "0 0 2 * * ?")
    public void detectChurn() {
        log.info("开始流失预警检测...");
        try {
            int count = churnAlertService.detectChurn();
            log.info("流失预警检测完成: 生成{}条预警", count);
        } catch (Exception e) {
            log.error("流失预警检测失败", e);
        }
    }

    /**
     * 每4小时检查一次超过48小时未跟进的预警，记录告警日志。
     * 运营主管应关注这些超时预警并及时处理。
     */
    @Scheduled(cron = "0 0 */4 * * ?")
    public void checkOverdueAlerts() {
        log.info("检查超时未跟进预警...");
        try {
            int overdueCount = churnAlertService.countOverdueAlerts(48);
            if (overdueCount > 0) {
                log.warn("流失预警超时未跟进: 有{}条pending预警超过48小时未处理，请运营主管关注！", overdueCount);
            } else {
                log.debug("流失预警跟进正常，无超时预警");
            }
        } catch (Exception e) {
            log.error("超时预警检查失败", e);
        }
    }
}
