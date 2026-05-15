package com.payflow.admin.interceptor;

import com.payflow.admin.config.JwtProperties;
import com.payflow.admin.service.CaptchaService;
import com.payflow.admin.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("JwtInterceptor preHandle: URI={}", request.getRequestURI());
        String authHeader = request.getHeader(jwtProperties.getHeader());

        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith(jwtProperties.getPrefix())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Missing or invalid Authorization header\",\"data\":null}");
            return false;
        }

        String token = authHeader.substring(jwtProperties.getPrefix().length());

        if (!jwtUtils.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Invalid or expired token\",\"data\":null}");
            return false;
        }

        // Store username & role in request for later use
        Claims claims = jwtUtils.parseToken(token);
        if (CaptchaService.CAPTCHA_JWT_SUBJECT.equals(claims.getSubject())) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"无效的访问令牌\",\"data\":null}");
            return false;
        }

        // 检查 JWT 黑名单
        String jti = claims.get("jti", String.class);
        if (jti != null) {
            try {
                Boolean blacklisted = stringRedisTemplate.hasKey("jwt:blacklist:" + jti);
                if (Boolean.TRUE.equals(blacklisted)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":401,\"message\":\"Token已失效\",\"data\":null}");
                    return false;
                }
            } catch (Exception e) {
                // Redis 不可用 → fail-close，拒绝请求以保证安全
                log.error("Redis黑名单检查失败（已拒绝）: {}", e.getMessage());
                response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":503,\"message\":\"服务暂不可用，请稍后重试\",\"data\":null}");
                return false;
            }
        }

        request.setAttribute("username", claims.getSubject());
        request.setAttribute("role", claims.get("role", String.class));
        Object dm = claims.get("dataMerchantIds");
        request.setAttribute("dataMerchantIds", dm != null ? dm.toString() : "");

        return true;
    }
}
