package com.payflow.admin.service;

import com.payflow.admin.config.AdminSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败计数与账号锁定（Redis，不可用时降级为进程内内存）。
 *
 * @author Lucas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginProtectionService {

    private static final String FAIL_KEY = "admin:login:fail:";
    private static final String LOCK_KEY = "admin:login:lock:";

    private final StringRedisTemplate stringRedisTemplate;
    private final AdminSecurityProperties adminSecurityProperties;

    /** Redis 不可用时的进程内降级存储 */
    private final ConcurrentHashMap<String, Integer> localFailCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> localLockUntilMs = new ConcurrentHashMap<>();

    /**
     * 读取当前连续登录失败次数（密码错误），未记录则为 0。
     */
    public int getFailureCount(String username) {
        String keyUser = normalizeUsername(username);
        if (!StringUtils.hasText(keyUser)) {
            return 0;
        }
        if (!isRedisAvailable()) {
            return localFailCounts.getOrDefault(keyUser, 0);
        }
        try {
            String raw = stringRedisTemplate.opsForValue().get(FAIL_KEY + keyUser);
            if (raw == null) {
                return 0;
            }
            return Math.max(0, Integer.parseInt(raw));
        } catch (Exception e) {
            log.warn("Redis 读取登录失败次数失败，使用内存降级: {}", e.getMessage());
            return localFailCounts.getOrDefault(keyUser, 0);
        }
    }

    /**
     * 是否需在登录时校验验证码：首次尝试（失败次数为 0）不需要；已有密码错误记录则需要。
     */
    public boolean isCaptchaRequired(String username) {
        if (!adminSecurityProperties.isLoginCaptchaEnabled()) {
            return false;
        }
        return getFailureCount(username) >= 1;
    }

    /**
     * 若账号已被锁定则抛出异常。
     */
    public void assertNotLocked(String username) {
        String keyUser = normalizeUsername(username);
        if (!StringUtils.hasText(keyUser)) {
            return;
        }
        if (!isRedisAvailable()) {
            assertNotLockedLocal(keyUser);
            return;
        }
        try {
            String lock = stringRedisTemplate.opsForValue().get(LOCK_KEY + keyUser);
            if (lock != null) {
                throw new IllegalArgumentException("登录尝试过多，账号已暂时锁定，请稍后再试");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis 读取账号锁定状态失败，使用内存降级: {}", e.getMessage());
            assertNotLockedLocal(keyUser);
        }
    }

    /**
     * 登录成功后清除失败计数。
     */
    public void clearFailures(String username) {
        String keyUser = normalizeUsername(username);
        localFailCounts.remove(keyUser);
        localLockUntilMs.remove(keyUser);
        if (!isRedisAvailable()) {
            return;
        }
        try {
            stringRedisTemplate.delete(FAIL_KEY + keyUser);
        } catch (Exception e) {
            log.warn("Redis 清除登录失败次数失败: {}", e.getMessage());
        }
    }

    /**
     * 登录密码错误时累加失败次数，达到阈值则锁定。
     */
    public void recordFailure(String username) {
        String keyUser = normalizeUsername(username);
        int max = Math.max(1, adminSecurityProperties.getLoginMaxFailures());
        int lockSec = Math.max(60, adminSecurityProperties.getLoginLockSeconds());
        if (!isRedisAvailable()) {
            recordFailureLocal(keyUser, max, lockSec);
            return;
        }
        String key = FAIL_KEY + keyUser;
        try {
            Long n = stringRedisTemplate.opsForValue().increment(key);
            if (n != null && n == 1L) {
                stringRedisTemplate.expire(key, Duration.ofSeconds(lockSec));
            }
            if (n != null && n >= max) {
                stringRedisTemplate.opsForValue().set(LOCK_KEY + keyUser, "1", Duration.ofSeconds(lockSec));
                stringRedisTemplate.delete(key);
            }
        } catch (Exception e) {
            log.warn("Redis 记录登录失败次数失败，使用内存降级: {}", e.getMessage());
            recordFailureLocal(keyUser, max, lockSec);
        }
    }

    private void assertNotLockedLocal(String keyUser) {
        Long until = localLockUntilMs.get(keyUser);
        if (until != null && until > System.currentTimeMillis()) {
            throw new IllegalArgumentException("登录尝试过多，账号已暂时锁定，请稍后再试");
        }
        if (until != null) {
            localLockUntilMs.remove(keyUser);
        }
    }

    private void recordFailureLocal(String keyUser, int max, int lockSec) {
        int n = localFailCounts.merge(keyUser, 1, Integer::sum);
        if (n >= max) {
            localLockUntilMs.put(keyUser, System.currentTimeMillis() + lockSec * 1000L);
            localFailCounts.remove(keyUser);
        }
    }

    private boolean isRedisAvailable() {
        try {
            stringRedisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }
}
