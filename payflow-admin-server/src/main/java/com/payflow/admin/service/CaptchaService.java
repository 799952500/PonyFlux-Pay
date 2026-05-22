package com.payflow.admin.service;

import com.payflow.admin.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import com.payflow.admin.util.JwtSigningKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录算术验证码：题目 operands 放在服务端签名的 JWT 中，校验时严格比较数值；
 * Redis 仅用于同一 captchaId（jti）一次性消费，防止重放。
 *
 * @author Lucas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    /** 与登录 JWT 区分 subject，避免验证码令牌被误当作会话令牌 */
    public static final String CAPTCHA_JWT_SUBJECT = "admin-captcha";

    private static final String KEY_ONCE_PREFIX = "admin:captcha:once:";
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final SecureRandom random = new SecureRandom();
    /** Redis 不可用时进程内一次性消费记录 */
    private final Set<String> localConsumedJti = ConcurrentHashMap.newKeySet();

    private SecretKey signingKey() {
        return JwtSigningKeys.hmacSha256(jwtProperties.getSecret());
    }

    /**
     * 签发验证码：captchaId 为短期 JWT（内含 a、b），前端展示 a+b。
     */
    public Map<String, String> issue() {
        int a = 1 + random.nextInt(9);
        int b = 1 + random.nextInt(9);
        Date now = new Date();
        Date exp = new Date(now.getTime() + CAPTCHA_TTL.toMillis());
        String jti = UUID.randomUUID().toString().replace("-", "");
        String token = Jwts.builder()
                .id(jti)
                .subject(CAPTCHA_JWT_SUBJECT)
                .claim("a", a)
                .claim("b", b)
                .issuedAt(now)
                .expiration(exp)
                .signWith(signingKey())
                .compact();
        return Map.of(
                "captchaId", token,
                "question", a + " + " + b + " = ?"
        );
    }

    /**
     * 校验答案：签名有效、未重复使用且数值等于 a+b。
     */
    public void validateAndConsume(String captchaToken, String userAnswer) {
        if (!StringUtils.hasText(captchaToken) || !StringUtils.hasText(userAnswer)) {
            throw new IllegalArgumentException("请输入验证码");
        }
        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(captchaToken)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new IllegalArgumentException("验证码已失效，请刷新");
        }
        if (!CAPTCHA_JWT_SUBJECT.equals(claims.getSubject())) {
            throw new IllegalArgumentException("验证码已失效，请刷新");
        }
        Integer a = claims.get("a", Integer.class);
        Integer b = claims.get("b", Integer.class);
        if (a == null || b == null) {
            throw new IllegalArgumentException("验证码已失效，请刷新");
        }
        String jti = claims.getId();
        if (!StringUtils.hasText(jti)) {
            throw new IllegalArgumentException("验证码已失效，请刷新");
        }
        Date exp = claims.getExpiration();
        long ttlSec = exp != null
                ? Math.max(1L, (exp.getTime() - System.currentTimeMillis()) / 1000L)
                : CAPTCHA_TTL.toSeconds();
        if (!markCaptchaConsumedOnce(jti, ttlSec)) {
            throw new IllegalArgumentException("验证码已失效，请刷新");
        }
        int expected = a + b;
        int parsed;
        try {
            parsed = Integer.parseInt(userAnswer.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("验证码错误");
        }
        if (parsed != expected) {
            throw new IllegalArgumentException("验证码错误");
        }
    }

    private boolean markCaptchaConsumedOnce(String jti, long ttlSec) {
        try {
            String lockKey = KEY_ONCE_PREFIX + jti;
            Boolean first = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(ttlSec));
            return Boolean.TRUE.equals(first);
        } catch (Exception e) {
            log.warn("Redis 验证码防重放失败，使用内存降级: {}", e.getMessage());
            return localConsumedJti.add(jti);
        }
    }
}
