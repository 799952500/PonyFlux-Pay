package com.payflow.cashier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 支付/退款通知去重：防止重复回调触发多次商户 Webhook。
 */
@Service
@RequiredArgsConstructor
public class NotifyDedupService {

    private static final String KEY_PREFIX = "notify:dedup:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试标记事件已处理。返回 true 表示首次处理（应继续发通知）；false 表示重复（应跳过）。
     */
    public boolean tryMark(String paymentId, String eventType) {
        if (paymentId == null || paymentId.isBlank() || eventType == null || eventType.isBlank()) {
            return true;
        }
        String key = KEY_PREFIX + paymentId + ":" + eventType;
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", DEFAULT_TTL);
        return Boolean.TRUE.equals(ok);
    }
}
