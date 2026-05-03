package com.payflow.admin.aspect;

import com.payflow.admin.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 对管理端写操作记录审计日志（不含请求体，避免泄露密码）。
 *
 * @author Lucas
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AdminAuditAspect {

    private final AuditLogService auditLogService;

    @Around("execution(* com.payflow.admin.controller..*.*(..))")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        String uri = request.getRequestURI();
        if (uri != null && (uri.contains("/auth/login") || uri.contains("/auth/captcha"))) {
            return joinPoint.proceed();
        }
        String method = request.getMethod();
        if (!"POST".equalsIgnoreCase(method) && !"PUT".equalsIgnoreCase(method)
                && !"DELETE".equalsIgnoreCase(method) && !"PATCH".equalsIgnoreCase(method)) {
            return joinPoint.proceed();
        }
        Object usernameAttr = request.getAttribute("username");
        String username = usernameAttr != null ? usernameAttr.toString() : "";
        try {
            Object result = joinPoint.proceed();
            auditLogService.record(username, method, uri,
                    joinPoint.getSignature().toShortString(), clientIp(request));
            return result;
        } catch (Throwable ex) {
            auditLogService.record(username, method, uri,
                    "异常: " + ex.getClass().getSimpleName() + " " + truncate(ex.getMessage(), 200),
                    clientIp(request));
            throw ex;
        }
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
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
