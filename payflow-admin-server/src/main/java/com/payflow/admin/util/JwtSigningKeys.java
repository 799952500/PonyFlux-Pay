package com.payflow.admin.util;

import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 将配置的 JWT 密钥规范为 HS256 所需的 ≥256 位 HMAC 密钥。
 * 长度不足 32 字节时通过 SHA-256 派生，避免 jjwt 抛出 WeakKeyException。
 */
public final class JwtSigningKeys {

    private static final int MIN_HMAC_KEY_BYTES = 32;

    private JwtSigningKeys() {
    }

    public static SecretKey hmacSha256(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= MIN_HMAC_KEY_BYTES) {
            return Keys.hmacShaKeyFor(raw);
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Keys.hmacShaKeyFor(digest.digest(raw));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
