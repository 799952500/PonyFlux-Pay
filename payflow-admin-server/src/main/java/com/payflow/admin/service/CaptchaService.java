package com.payflow.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * 登录算术验证码（Redis 存储正确答案）。
 *
 * @author Lucas
 */
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final String KEY_PREFIX = "admin:captcha:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom random = new SecureRandom();

    /**
     * 签发验证码，返回题目展示文案与 captchaId。
     */
    public Map<String, String> issue() {
        int a = 1 + random.nextInt(9);
        int b = 1 + random.nextInt(9);
        String answer = String.valueOf(a + b);
        String id = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + id, answer, TTL);
        return Map.of(
                "captchaId", id,
                "question", a + " + " + b + " = ?"
        );
    }

    /**
     * 校验答案（校验成功后删除 Redis 键，防止重放）。
     */
    public void validateAndConsume(String captchaId, String userAnswer) {
        if (!StringUtils.hasText(captchaId) || !StringUtils.hasText(userAnswer)) {
            throw new IllegalArgumentException("请输入验证码");
        }
        String key = KEY_PREFIX + captchaId;
        String expected = stringRedisTemplate.opsForValue().get(key);
        if (!StringUtils.hasText(expected)) {
            throw new IllegalArgumentException("验证码已失效，请刷新");
        }
        stringRedisTemplate.delete(key);
        if (!expected.trim().equals(userAnswer.trim())) {
            throw new IllegalArgumentException("验证码错误");
        }
    }
}
