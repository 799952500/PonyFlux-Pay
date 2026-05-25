package com.payflow.cashier.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.context.MerchantScopeHolder;
import com.payflow.cashier.entity.WebhookDeliveryLog;
import com.payflow.cashier.mapper.WebhookDeliveryLogMapper;
import com.payflow.cashier.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Webhook 投递重试定时任务——每分钟扫描 PENDING 状态的投递记录并重试。
 *
 * <p>重试策略：指数退避 60s * 2^attempt，最多 {{@link WebhookDeliveryLog#MAX_RETRY}} 次。</p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookRetryTask {

    private final WebhookDeliveryLogMapper deliveryLogMapper;
    private final WebhookDeliveryService deliveryService;

    /** 基础重试间隔（秒） */
    private static final long BASE_RETRY_SECONDS = 60;

    @Scheduled(fixedDelay = 60_000, initialDelay = 120_000)
    public void retryPendingDeliveries() {
        MerchantScopeHolder.runInSystemMode(this::retryPendingDeliveriesInternal);
    }

    private void retryPendingDeliveriesInternal() {
        log.debug("Webhook重试扫描开始...");
        try {
            List<WebhookDeliveryLog> pending = deliveryLogMapper.selectList(
                    new LambdaQueryWrapper<WebhookDeliveryLog>()
                            .eq(WebhookDeliveryLog::getStatus, WebhookDeliveryLog.STATUS_PENDING));

            if (pending.isEmpty()) {
                return;
            }

            int retried = 0;
            for (WebhookDeliveryLog entry : pending) {
                if (!shouldRetryNow(entry)) {
                    continue;
                }
                try {
                    boolean success = deliveryService.retry(entry);
                    if (success) {
                        retried++;
                    }
                } catch (Exception e) {
                    log.error("Webhook重试异常: deliveryId={}", entry.getId(), e);
                }
            }

            if (retried > 0) {
                log.info("Webhook重试完成: 成功重试={}, 剩余待处理={}", retried, pending.size() - retried);
            }
        } catch (Exception e) {
            log.error("Webhook重试扫描异常", e);
        }
    }

    /**
     * 计算指数退避时间，判断当前是否应重试。
     */
    private boolean shouldRetryNow(WebhookDeliveryLog entry) {
        int attempt = entry.getAttempt() != null ? entry.getAttempt() : 0;
        long delaySeconds = BASE_RETRY_SECONDS * (1L << attempt); // 60s * 2^attempt
        LocalDateTime createdAt = entry.getCreatedAt();
        if (createdAt == null) {
            return true;
        }
        LocalDateTime nextRetryAt = createdAt.plusSeconds(delaySeconds);
        return LocalDateTime.now().isAfter(nextRetryAt);
    }
}
