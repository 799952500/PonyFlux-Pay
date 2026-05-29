package com.payflow.admin.util;

import com.payflow.admin.config.JwtProperties;
import com.payflow.common.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
/**
 * @author Lucas
 */
public class JwtUtils {

    private final JwtProperties jwtProperties;
    private final JwtService jwtService;

    public JwtUtils(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.jwtService = new JwtService(jwtProperties.getSecret(), jwtProperties.getExpiration());
    }

    public String generateToken(String username, String role, String dataMerchantIds) {
        Map<String, Object> claims = new HashMap<>(5);
        claims.put("role", role != null ? role : "");
        claims.put("merchantId", "");
        claims.put("dataMerchantIds", dataMerchantIds != null ? dataMerchantIds : "");
        return jwtService.generateToken(username, claims);
    }

    public Claims parseToken(String token) {
        return jwtService.parseToken(token);
    }

    public boolean validateToken(String token) {
        return jwtService.validateToken(token);
    }

    public String getUsername(String token) {
        return parseToken(token).getSubject();
    }

    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    public Date getExpiration(String token) {
        return parseToken(token).getExpiration();
    }

    public String getJti(String token) {
        return jwtService.getJti(token);
    }
}
