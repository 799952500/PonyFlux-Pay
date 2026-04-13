package com.payflow.cashier.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.config.PayflowProperties;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * JWT 认证拦截器
 * <p>
 * 拦截所有 /api/v1/orders/** 和 /api/v1/payments/** 请求，
 * 从请求头 Authorization: Bearer <token> 中提取并验证 JWT。
 * 验证通过后将 merchantId 注入到请求属性中。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    /** 请求属性 Key：存放已验证的商户号 */
    public static final String ATTR_MERCHANT_ID = "merchantId";

    private final PayflowProperties properties;
    private final ObjectMapper objectMapper;

    public JwtAuthInterceptor(PayflowProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
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

        String merchantId = JwtUtils.verifyAndGetMerchantId(
                properties.getJwt().getSecret(), token);

        if (merchantId == null) {
            log.warn("JWT认证失败: token无效或已过期, path={}", request.getRequestURI());
            sendUnauthorized(response, "token无效或已过期");
            return false;
        }

        // 将 merchantId 注入到请求上下文
        request.setAttribute(ATTR_MERCHANT_ID, merchantId);
        log.debug("JWT认证成功: merchantId={}, path={}", merchantId, request.getRequestURI());
        return true;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        R<?> r = R.unauthorized(message);
        objectMapper.writeValue(response.getWriter(), r);
    }
}
