package com.payflow.admin.interceptor;

import com.payflow.admin.config.PermissionProperties;
import com.payflow.admin.security.LogicalOp;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.security.RequireRole;
import com.payflow.admin.service.PermissionQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;

/**
 * 按钮权限拦截器——校验 {@link RequirePermission} 注解。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionInterceptor implements HandlerInterceptor {

    private final PermissionQueryService permissionQueryService;
    private final PermissionProperties permissionProperties;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            return true;
        }

        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");
        if (!StringUtils.hasText(username)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    1002, "未授权访问：请先登录");
            return false;
        }

        if (RequireRole.SUPER_ADMIN.equals(role)) {
            return true;
        }

        String[] required = requirePermission.value();
        if (required.length == 0) {
            return true;
        }

        Set<String> owned = permissionQueryService.getPermCodesByUsername(username);
        boolean allowed = requirePermission.logical() == LogicalOp.OR
                ? Arrays.stream(required).anyMatch(owned::contains)
                : Arrays.stream(required).allMatch(owned::contains);

        if (allowed) {
            return true;
        }

        log.warn("PermissionInterceptor: 用户 {} 无权限访问 {}，所需: {}",
                username, request.getRequestURI(), Arrays.toString(required));

        if (!permissionProperties.isEnforceButton()) {
            log.warn("PermissionInterceptor: enforce-button=false，仅记录不拒绝");
            return true;
        }

        writeJson(response, HttpServletResponse.SC_FORBIDDEN,
                1003, "无操作权限");
        return false;
    }

    private static void writeJson(HttpServletResponse response, int httpStatus, int code, String message)
            throws Exception {
        response.setStatus(httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"code\":%d,\"message\":\"%s\",\"data\":null}", code, message));
    }
}
