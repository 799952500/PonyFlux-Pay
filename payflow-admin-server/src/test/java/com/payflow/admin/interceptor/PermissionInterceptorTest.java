package com.payflow.admin.interceptor;

import com.payflow.admin.config.PermissionProperties;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.service.PermissionQueryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionInterceptorTest {

    @Mock
    private PermissionQueryService permissionQueryService;

    @Mock
    private HttpServletRequest request;

    private PermissionProperties permissionProperties;
    private PermissionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        permissionProperties = new PermissionProperties();
        permissionProperties.setEnforceButton(true);
        interceptor = new PermissionInterceptor(permissionQueryService, permissionProperties);
    }

    @Test
    void superAdminBypassesPermissionCheck() throws Exception {
        when(request.getAttribute("role")).thenReturn("SUPER_ADMIN");
        when(request.getAttribute("username")).thenReturn("admin");

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod("refund:approve"));
        assertTrue(allowed);
    }

    @Test
    void deniesWhenPermissionMissing() throws Exception {
        when(request.getAttribute("role")).thenReturn("FINANCE");
        when(request.getAttribute("username")).thenReturn("finance_demo");
        when(request.getRequestURI()).thenReturn("/api/v1/admin/refunds/R1/approve");
        when(permissionQueryService.getPermCodesByUsername(eq("finance_demo"))).thenReturn(Set.of("refund:reject"));

        MockHttpServletResponse response = new MockHttpServletResponse();
        boolean allowed = interceptor.preHandle(request, response, handlerMethod("refund:approve"));

        assertFalse(allowed);
        assertTrue(response.getContentAsString().contains("无操作权限"));
    }

    @Test
    void allowsWhenPermissionPresent() throws Exception {
        when(request.getAttribute("role")).thenReturn("FINANCE");
        when(request.getAttribute("username")).thenReturn("finance_demo");
        when(permissionQueryService.getPermCodesByUsername(eq("finance_demo"))).thenReturn(Set.of("refund:approve"));

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod("refund:approve"));
        assertTrue(allowed);
    }

    @Test
    void enforceOffLogsOnly() throws Exception {
        permissionProperties.setEnforceButton(false);
        when(request.getAttribute("role")).thenReturn("ADMIN");
        when(request.getAttribute("username")).thenReturn("ops");
        when(request.getRequestURI()).thenReturn("/api/v1/admin/roles/1");
        when(permissionQueryService.getPermCodesByUsername(eq("ops"))).thenReturn(Set.of());

        boolean allowed = interceptor.preHandle(request, new MockHttpServletResponse(), handlerMethod("role:delete"));
        assertTrue(allowed);
    }

    private static HandlerMethod handlerMethod(String ignored) throws NoSuchMethodException {
        Method method = DummyController.class.getDeclaredMethod("secured");
        return new HandlerMethod(new DummyController(), method);
    }

    static class DummyController {
        @RequirePermission("refund:approve")
        public void secured() {
        }
    }
}
