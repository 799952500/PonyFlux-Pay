package com.payflow.admin.interceptor;

import com.payflow.admin.config.JwtProperties;
import com.payflow.admin.service.CaptchaService;
import com.payflow.admin.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtProperties jwtProperties;
    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
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
        request.setAttribute("username", claims.getSubject());
        request.setAttribute("role", claims.get("role", String.class));
        Object dm = claims.get("dataMerchantIds");
        request.setAttribute("dataMerchantIds", dm != null ? dm.toString() : "");

        return true;
    }
}
