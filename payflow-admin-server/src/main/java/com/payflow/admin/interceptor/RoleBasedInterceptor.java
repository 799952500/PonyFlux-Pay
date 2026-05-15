package com.payflow.admin.interceptor;

import com.payflow.admin.security.RequireRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 基于角色的访问控制拦截器——校验 {@link RequireRole} 注解。
 *
 * <p>在 JwtInterceptor 之后执行，读取 {@code request.getAttribute("role")}
 * 并与方法上 {@code @RequireRole} 声明的角色列表比对。</p>
 *
 * <p>规则：
 * <ul>
 *   <li>未标注 @RequireRole 的方法 → 所有已认证用户可访问</li>
 *   <li>SUPER_ADMIN → 拥有全部权限</li>
 *   <li>其他角色 → 必须出现在注解声明的角色列表中</li>
 * </ul>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class RoleBasedInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
        if (requireRole == null) {
            return true;
        }

        String role = (String) request.getAttribute("role");
        if (role == null) {
            log.warn("RoleBasedInterceptor: 未认证用户尝试访问受保护资源 {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":1002,\"message\":\"未授权访问：请先登录\",\"data\":null}");
            return false;
        }

        // SUPER_ADMIN 拥有全部权限
        if (RequireRole.SUPER_ADMIN.equals(role)) {
            return true;
        }

        Set<String> allowedRoles = new HashSet<>(Arrays.asList(requireRole.value()));
        if (allowedRoles.contains(role)) {
            return true;
        }

        log.warn("RoleBasedInterceptor: 角色 {} 无权限访问 {}，所需角色: {}",
                role, request.getRequestURI(), allowedRoles);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":1003,\"message\":\"权限不足：此操作需要超级管理员权限\",\"data\":null}");
        return false;
    }
}
