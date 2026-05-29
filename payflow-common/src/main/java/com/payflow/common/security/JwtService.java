package com.payflow.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 统一 JWT 签发与校验（含 jti）。
 */
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(String secret, long expirationMs) {
        this.signingKey = JwtSigningKeys.hmacSha256(secret);
        this.expirationMs = expirationMs;
    }

    public String generateToken(String subject, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        Map<String, Object> claims = new HashMap<>(extraClaims != null ? extraClaims : Map.of());
        claims.putIfAbsent("jti", UUID.randomUUID().toString());
        return Jwts.builder()
                .subject(subject)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getJti(String token) {
        return parseToken(token).get("jti", String.class);
    }
}
