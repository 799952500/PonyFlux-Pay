package com.payflow.cashier.routing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
/**
 * 渠道账户调用健康度（滑动窗口近似：按账户累计成功/失败计数，用于智能路由降权）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelHealthRedisService {

    private static final String KEY_OK = "payflow:route:ok:";
    private static final String KEY_FAIL = "payflow:route:fail:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 记录一次渠道调用结果。
     */
    public void recordOutcome(String accountCode, boolean success) {
        if (accountCode == null || accountCode.isBlank()) {
            return;
        }
        String k = success ? KEY_OK + accountCode : KEY_FAIL + accountCode;
        try {
            stringRedisTemplate.opsForValue().increment(k);
            stringRedisTemplate.expire(k, Duration.ofHours(24));
        } catch (Exception e) {
            log.warn("记录渠道健康度失败: accountCode={}, success={}, error={}", accountCode, success, e.getMessage());
        }
    }

    /**
     * 失败率 0~1；无数据返回 0。
     */
    public double failureRate(String accountCode) {
        if (accountCode == null || accountCode.isBlank()) {
            return 0.0;
        }
        try {
            String okStr = stringRedisTemplate.opsForValue().get(KEY_OK + accountCode);
            String failStr = stringRedisTemplate.opsForValue().get(KEY_FAIL + accountCode);
            long ok = parseLong(okStr);
            long fail = parseLong(failStr);
            long total = ok + fail;
            if (total <= 0) {
                return 0.0;
            }
            return (double) fail / (double) total;
        } catch (Exception e) {
            log.warn("读取渠道健康度失败: accountCode={}, error={}", accountCode, e.getMessage());
            return 0.0;
        }
    }

    /**
     * 熔断：失败率超过阈值且样本足够时视为不可用。
     */
    public boolean isCircuitOpen(String accountCode, double threshold, int minSamples) {
        if (accountCode == null || accountCode.isBlank()) {
            return false;
        }
        try {
            String okStr = stringRedisTemplate.opsForValue().get(KEY_OK + accountCode);
            String failStr = stringRedisTemplate.opsForValue().get(KEY_FAIL + accountCode);
            long ok = parseLong(okStr);
            long fail = parseLong(failStr);
            long total = ok + fail;
            if (total < minSamples) {
                return false;
            }
            return (double) fail / (double) total >= threshold;
        } catch (Exception e) {
            return false;
        }
    }

    private static long parseLong(String s) {
        if (s == null || s.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
