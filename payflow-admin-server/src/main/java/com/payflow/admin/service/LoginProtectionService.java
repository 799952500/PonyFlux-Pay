package com.payflow.admin.service;

import com.payflow.admin.config.AdminSecurityProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败计数与账号锁定（Redis）。
 *
 * @author Lucas
 */
@Service
@RequiredArgsConstructor
public class LoginProtectionService {

    private static final String FAIL_KEY = "admin:login:fail:";
    private static final String LOCK_KEY = "admin:login:lock:";

    private final StringRedisTemplate stringRedisTemplate;
    private final AdminSecurityProperties adminSecurityProperties;

    /**
     * 若账号已被锁定则抛出异常。
     */
    public void assertNotLocked(String username) {
        String lock = stringRedisTemplate.opsForValue().get(LOCK_KEY + username);
        if (lock != null) {
            throw new IllegalArgumentException("登录尝试过多，账号已暂时锁定，请稍后再试");
        }
    }

    /**
     * 登录成功后清除失败计数。
     */
    public void clearFailures(String username) {
        stringRedisTemplate.delete(FAIL_KEY + username);
    }

    /**
     * 登录密码错误时累加失败次数，达到阈值则锁定。
     */
    public void recordFailure(String username) {
        int max = Math.max(1, adminSecurityProperties.getLoginMaxFailures());
        int lockSec = Math.max(60, adminSecurityProperties.getLoginLockSeconds());
        String key = FAIL_KEY + username;
        Long n = stringRedisTemplate.opsForValue().increment(key);
        if (n != null && n == 1L) {
            stringRedisTemplate.expire(key, Duration.ofSeconds(lockSec));
        }
        if (n != null && n >= max) {
            stringRedisTemplate.opsForValue().set(LOCK_KEY + username, "1", Duration.ofSeconds(lockSec));
            stringRedisTemplate.delete(key);
        }
    }
}
