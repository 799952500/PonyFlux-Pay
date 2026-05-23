package com.payflow.cashier.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 公网入驻接口 IP 限频。
 */
@Service
@RequiredArgsConstructor
public class OnboardingPublicRateLimitService {

    private static final int SUBMIT_MAX = 3;
    private static final Duration WINDOW = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;

    public void assertSubmitAllowed(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }
        String key = "onb:cashier:submit:ip:" + clientIp.trim();
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, WINDOW);
        }
        if (count != null && count > SUBMIT_MAX) {
            throw new IllegalArgumentException("提交过于频繁，请稍后再试");
        }
    }
}
