package com.payflow.admin.interceptor;

import com.payflow.admin.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

/**
 * 管理端写操作审计：在 JWT 拦截器之后执行，可从 request 读取 username。
 * 登录接口由 {@link com.payflow.admin.service.impl.AdminAuthServiceImpl} 单独写「登录成功/失败」日志，此处跳过以免重复。
 * <p>
 * 说明：过滤器阶段尚未经过 JwtInterceptor，无法可靠得到当前用户，故采用 HandlerInterceptor 而非 Filter。
 *
 * @author Lucas
 */
@Component
@RequiredArgsConstructor
public class AdminAuditInterceptor implements HandlerInterceptor {

    private final AuditLogService auditLogService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        if (!(handler instanceof HandlerMethod hm)) {
            return;
        }
        String verb = request.getMethod();
        if (!isMutating(verb)) {
            return;
        }
        String uri = request.getRequestURI();
        if (shouldSkip(uri)) {
            return;
        }
        String username = Optional.ofNullable(request.getAttribute("username"))
                .map(Object::toString)
                .orElse("");
        String detail = buildDetail(hm, ex);
        auditLogService.record(username, verb, uri, detail, clientIp(request));
    }

    private static boolean isMutating(String m) {
        return "POST".equalsIgnoreCase(m)
                || "PUT".equalsIgnoreCase(m)
                || "DELETE".equalsIgnoreCase(m)
                || "PATCH".equalsIgnoreCase(m);
    }

    private static boolean shouldSkip(String uri) {
        if (uri == null) {
            return true;
        }
        return uri.contains("/auth/login")
                || uri.contains("/auth/captcha")
                || uri.contains("/meta/");
    }

    private static String buildDetail(HandlerMethod hm, Exception ex) {
        String base = hm.getBeanType().getSimpleName() + "." + hm.getMethod().getName() + "()";
        if (ex != null) {
            return base + " | 异常: " + ex.getClass().getSimpleName() + " " + truncate(ex.getMessage(), 180);
        }
        return base;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String truncate(String m, int max) {
        if (m == null) {
            return "";
        }
        return m.length() <= max ? m : m.substring(0, max);
    }
}
