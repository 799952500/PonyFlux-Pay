package com.payflow.admin.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.ChurnAlert;
import com.payflow.admin.enums.NotificationTypeEnum;
import com.payflow.admin.mapper.ChurnAlertMapper;
import com.payflow.admin.service.ChurnAlertService;
import com.payflow.admin.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
    private final ChurnAlertMapper churnAlertMapper;
    private final NotificationService notificationService;

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
     * 每4小时检查一次超过48小时未跟进的预警，记录告警日志并发送站内通知。
     * 运营主管应关注这些超时预警并及时处理。
     */
    @Scheduled(cron = "0 0 */4 * * ?")
    public void checkOverdueAlerts() {
        log.info("检查超时未跟进预警...");
        try {
            int overdueCount = churnAlertService.countOverdueAlerts(48);
            if (overdueCount > 0) {
                log.warn("流失预警超时未跟进: 有{}条pending预警超过48小时未处理，请运营主管关注！", overdueCount);
                sendOverdueNotifications();
            } else {
                log.debug("流失预警跟进正常，无超时预警");
            }
        } catch (Exception e) {
            log.error("超时预警检查失败", e);
        }
    }

    /**
     * 查询超时 alert 列表并逐条发送站内通知。
     */
    private void sendOverdueNotifications() {
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(48);
            List<ChurnAlert> overdueAlerts = churnAlertMapper.selectList(
                    new LambdaQueryWrapper<ChurnAlert>()
                            .eq(ChurnAlert::getStatus, "pending")
                            .lt(ChurnAlert::getCreateTime, cutoff));
            for (ChurnAlert alert : overdueAlerts) {
                String merchantIdStr = String.valueOf(alert.getMerchantId());
                String title = "流失预警超时未跟进";
                String summary = "商户 " + (alert.getMerchantName() != null ? alert.getMerchantName() : merchantIdStr)
                        + " 的流失预警已超过48小时未处理，预警等级: " + alert.getAlertLevel();
                String link = "/admin/churn-alerts?status=pending";
                notificationService.sendToRole(
                        NotificationTypeEnum.CHURN_OVERDUE,
                        "CHURN-" + alert.getId(),
                        title, summary, link, merchantIdStr,
                        "churn:manage");
            }
        } catch (Exception e) {
            log.error("发送流失预警超时通知失败", e);
        }
    }
}
