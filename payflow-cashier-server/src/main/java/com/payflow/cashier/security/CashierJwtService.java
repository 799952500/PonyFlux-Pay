package com.payflow.cashier.security;

import com.payflow.cashier.config.PayflowProperties;
import com.payflow.common.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 收银台 JWT 签发与校验（基于 payflow-common {@link JwtService}）。
 */
@Component
public class CashierJwtService {

    private static final String CLAIM_MERCHANT_ID = "merchantId";
    private static final String CLAIM_MERCHANT_NAME = "merchantName";

    private final JwtService jwtService;

    public CashierJwtService(PayflowProperties properties) {
        this.jwtService = new JwtService(
                properties.getJwt().getSecret(),
                properties.getJwt().getExpireSeconds() * 1000L);
    }

    public String generateToken(String merchantId, String merchantName) {
        Map<String, Object> claims = new HashMap<>(4);
        claims.put(CLAIM_MERCHANT_ID, merchantId);
        claims.put(CLAIM_MERCHANT_NAME, merchantName);
        return jwtService.generateToken(merchantId, claims);
    }

    public String verifyAndGetMerchantId(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            return claims.get(CLAIM_MERCHANT_ID, String.class);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public TokenClaims parseClaims(String token) {
        try {
            Claims claims = jwtService.parseToken(token);
            return new TokenClaims(
                    claims.get(CLAIM_MERCHANT_ID, String.class),
                    claims.get(CLAIM_MERCHANT_NAME, String.class),
                    claims.getExpiration(),
                    claims.get("jti", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public static String calcExpireTimeStr(long expireSeconds) {
        Instant expiry = Instant.now().plusSeconds(expireSeconds);
        return expiry.atOffset(ZoneOffset.ofHours(8))
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    public record TokenClaims(String merchantId, String merchantName, Date expiration, String jti) {
    }
}
