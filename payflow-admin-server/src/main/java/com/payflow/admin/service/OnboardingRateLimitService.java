package com.payflow.admin.service;

import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 入驻申请提交与结果查询限频（Redis）。
 */
@Service
@RequiredArgsConstructor
public class OnboardingRateLimitService {

    private static final int RATE_LIMIT_CODE = 6208;

    private static final int SUBMIT_MAX_PER_IP = 3;
    private static final Duration SUBMIT_WINDOW = Duration.ofMinutes(10);

    private static final int RESULT_FAIL_MAX = 3;
    private static final Duration RESULT_FAIL_COOLDOWN = Duration.ofMinutes(30);

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 公网提交申请：单 IP 10 分钟最多 3 次。
     */
    public void assertSubmitAllowed(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return;
        }
        String key = "onb:submit:ip:" + clientIp.trim();
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, SUBMIT_WINDOW);
        }
        if (count != null && count > SUBMIT_MAX_PER_IP) {
            throw new BizException(RATE_LIMIT_CODE, "提交过于频繁，请稍后再试");
        }
    }

    /**
     * 联系方式校验失败累计，3 次后冷却 30 分钟。
     */
    public void recordResultContactMismatch(String applicationNo) {
        String key = "onb:result:fail:" + applicationNo;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, RESULT_FAIL_COOLDOWN);
        }
    }

    /**
     * 校验是否处于联系方式失败冷却期。
     */
    public void assertResultNotLocked(String applicationNo) {
        String key = "onb:result:lock:" + applicationNo;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
            throw new BizException(RATE_LIMIT_CODE, "查询失败次数过多，请 30 分钟后再试");
        }
        String failKey = "onb:result:fail:" + applicationNo;
        String val = stringRedisTemplate.opsForValue().get(failKey);
        if (val != null) {
            int fails = Integer.parseInt(val);
            if (fails >= RESULT_FAIL_MAX) {
                stringRedisTemplate.opsForValue().set(key, "1", RESULT_FAIL_COOLDOWN);
                stringRedisTemplate.delete(failKey);
                throw new BizException(RATE_LIMIT_CODE, "查询失败次数过多，请 30 分钟后再试");
            }
        }
    }

    /**
     * 查询成功则清除失败计数。
     */
    public void clearResultFailures(String applicationNo) {
        stringRedisTemplate.delete("onb:result:fail:" + applicationNo);
        stringRedisTemplate.delete("onb:result:lock:" + applicationNo);
    }
}
