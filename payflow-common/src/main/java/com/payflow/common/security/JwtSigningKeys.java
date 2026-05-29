package com.payflow.common.security;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * JWT HMAC 签名密钥工具。
 */
public final class JwtSigningKeys {

    private JwtSigningKeys() {
    }

    public static SecretKey hmacSha256(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("JWT secret 不能为空");
        }
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }
}
