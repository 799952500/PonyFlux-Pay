package com.payflow.admin.task;

import com.payflow.admin.dto.recon.ReconLongTailSummaryDTO;
import com.payflow.admin.enums.NotificationTypeEnum;
import com.payflow.admin.service.NotificationService;
import com.payflow.admin.service.recon.ReconLongTailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 长尾差异每日摘要（09:00）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconLongTailDigestTask {

    private final ReconLongTailService reconLongTailService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDigest() {
        try {
            ReconLongTailSummaryDTO summary = reconLongTailService.buildSummary(LocalDate.now(), null);
            long gt30 = summary.getBuckets().stream()
                    .filter(b -> "GT_30".equals(b.getAgeBucket()))
                    .mapToLong(ReconLongTailSummaryDTO.Bucket::getDiffCount)
                    .sum();
            if (gt30 <= 0) {
                return;
            }
            notificationService.sendToRole(
                    NotificationTypeEnum.RECON_DIFF_LONG_TAIL,
                    "long-tail-" + LocalDate.now(),
                    "长尾差异摘要",
                    "当前超过 30 天未关闭差异 " + gt30 + " 笔，最大账龄 " + summary.getMaxAgeDays() + " 天",
                    "/admin/reconcile/long-tail",
                    null,
                    "recon:manage");
            log.info("长尾摘要已推送 gt30={}", gt30);
        } catch (Exception e) {
            log.error("长尾摘要推送失败", e);
        }
    }
}
