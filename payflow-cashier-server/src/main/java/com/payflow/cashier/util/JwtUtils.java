package com.payflow.cashier.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * JWT 工具类
 * 使用 HS256 算法签名与验签
 *
 * @author PayFlow Team
 */
@Slf4j
public final class JwtUtils {

    private static final String ALGORITHM = "HS256";
    private static final String CLAIM_MERCHANT_ID = "merchantId";
    private static final String CLAIM_MERCHANT_NAME = "merchantName";

    private JwtUtils() {
        // 工具类禁止实例化
    }

    /**
     * 生成 JWT Token
     *
     * @param secret       签名密钥
     * @param merchantId   商户号
     * @param merchantName 商户名称
     * @param expireSeconds 有效期（秒）
     * @return JWT Token 字符串
     */
    public static String generateToken(String secret, String merchantId, String merchantName, long expireSeconds) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expireSeconds);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject(merchantId)
                .claim(CLAIM_MERCHANT_ID, merchantId)
                .claim(CLAIM_MERCHANT_NAME, merchantName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        log.debug("生成Token: merchantId={}, expireSeconds={}", merchantId, expireSeconds);
        return token;
    }

    /**
     * 验证 JWT Token 并解析商户号
     *
     * @param secret 签名密钥
     * @param token  JWT Token
     * @return merchantId，验证失败返回 null
     */
    public static String verifyAndGetMerchantId(String secret, String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.get(CLAIM_MERCHANT_ID, String.class);
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 Token 获取商户信息
     *
     * @param secret 签名密钥
     * @param token  JWT Token
     * @return TokenClaims，解析失败返回 null
     */
    public static TokenClaims parseClaims(String secret, String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new TokenClaims(
                    claims.get(CLAIM_MERCHANT_ID, String.class),
                    claims.get(CLAIM_MERCHANT_NAME, String.class),
                    claims.getExpiration()
            );
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.warn("Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 计算 Token 过期时间（格式化字符串）
     *
     * @param expireSeconds 有效期（秒）
     * @return ISO 8601 格式时间字符串
     */
    public static String calcExpireTimeStr(long expireSeconds) {
        Instant expiry = Instant.now().plusSeconds(expireSeconds);
        return expiry.atOffset(ZoneOffset.ofHours(8))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Token 声明对象
     */
    public record TokenClaims(String merchantId, String merchantName, Date expiration) {
    }
}
