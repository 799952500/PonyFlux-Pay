package com.payflow.admin.redis;

import com.payflow.common.redis.RedisTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 收银台配置刷新事件发布器（Redis Pub/Sub）。
 *
 * @author Lucas
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payflow.cache.redis.enabled", havingValue = "true", matchIfMissing = false)
public class CashierConfigRefreshPublisher {

    private final StringRedisTemplate stringRedisTemplate;

    public void publish(String reason) {
        String payload = String.format("{\"reason\":\"%s\",\"ts\":%d}",
                reason == null ? "" : reason, Instant.now().getEpochSecond());
        stringRedisTemplate.convertAndSend(RedisTopics.CASHIER_CONFIG_REFRESH, payload);
        log.info("发布收银台配置刷新事件: reason={}", reason);
    }
}

