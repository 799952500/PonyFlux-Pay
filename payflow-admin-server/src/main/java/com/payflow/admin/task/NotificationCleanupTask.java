package com.payflow.admin.task;

import com.payflow.admin.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 站内通知清理定时任务（每日凌晨3:00清理90天前的已读通知）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupTask {

    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupOldNotifications() {
        try {
            int deleted = notificationService.cleanupOldRead(90);
            if (deleted > 0) {
                log.info("已清理 {} 条过期已读通知", deleted);
            }
        } catch (Exception e) {
            log.error("清理过期通知失败", e);
        }
    }
}
