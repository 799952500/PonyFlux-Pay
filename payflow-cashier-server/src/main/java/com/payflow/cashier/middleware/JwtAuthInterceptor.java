package com.payflow.cashier.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.context.AuthMode;
import com.payflow.cashier.security.CashierJwtService;
import com.payflow.common.web.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 认证拦截器
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_MERCHANT_ID = "merchantId";

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final CashierJwtService cashierJwtService;

    public JwtAuthInterceptor(ObjectMapper objectMapper,
                              StringRedisTemplate stringRedisTemplate,
                              CashierJwtService cashierJwtService) {
        this.objectMapper = objectMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cashierJwtService = cashierJwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("JWT认证失败: 缺少或格式错误的 Authorization 头, path={}", request.getRequestURI());
            sendUnauthorized(response, "缺少 Authorization 头或格式错误");
            return false;
        }

        String token = authHeader.substring(7);
        if (token.isBlank()) {
            log.warn("JWT认证失败: token 为空, path={}", request.getRequestURI());
            sendUnauthorized(response, "token 不能为空");
            return false;
        }

        String merchantId = cashierJwtService.verifyAndGetMerchantId(token);
        if (merchantId == null) {
            log.warn("JWT认证失败: token无效或已过期, path={}", request.getRequestURI());
            sendUnauthorized(response, "token无效或已过期");
            return false;
        }

        CashierJwtService.TokenClaims parsedClaims = cashierJwtService.parseClaims(token);
        if (parsedClaims != null && parsedClaims.jti() != null) {
            try {
                Boolean blacklisted = stringRedisTemplate.hasKey("jwt:blacklist:" + parsedClaims.jti());
                if (Boolean.TRUE.equals(blacklisted)) {
                    log.warn("JWT认证失败: Token已登出, merchantId={}, path={}", merchantId, request.getRequestURI());
                    sendUnauthorized(response, "Token已失效");
                    return false;
                }
            } catch (Exception e) {
                log.error("Redis黑名单检查失败（已拒绝）: {}", e.getMessage());
                sendUnauthorized(response, "服务暂不可用，请稍后重试");
                return false;
            }
        }

        request.setAttribute(ATTR_MERCHANT_ID, merchantId);
        request.setAttribute(MerchantContextInterceptor.ATTR_AUTH_MODE, AuthMode.JWT);
        log.debug("JWT认证成功: merchantId={}, path={}", merchantId, request.getRequestURI());
        return true;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), R.unauthorized(message));
    }
}
