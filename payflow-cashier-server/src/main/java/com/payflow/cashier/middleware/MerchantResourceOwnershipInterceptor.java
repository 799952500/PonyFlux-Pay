package com.payflow.cashier.middleware;

import com.payflow.cashier.service.ResourceOwnershipService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * 按路径变量校验资源所有权。
 *
 * @author PayFlow Team
 */
@Component
@RequiredArgsConstructor
public class MerchantResourceOwnershipInterceptor implements HandlerInterceptor {

    private final ResourceOwnershipService resourceOwnershipService;

    @Override
    @SuppressWarnings("unchecked")
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Map<String, String> pathVars = (Map<String, String>) request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (pathVars == null || pathVars.isEmpty()) {
            return true;
        }
        String method = request.getMethod();
        String path = request.getRequestURI();
        String clientIp = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        if (pathVars.containsKey("orderId")) {
            resourceOwnershipService.assertOrderOwned(pathVars.get("orderId"), method, path, clientIp, userAgent);
        }
        if (pathVars.containsKey("paymentId")) {
            resourceOwnershipService.assertPaymentOwned(pathVars.get("paymentId"), method, path, clientIp, userAgent);
        }
        if (pathVars.containsKey("refundId")) {
            resourceOwnershipService.assertRefundOwned(pathVars.get("refundId"), method, path, clientIp, userAgent);
        }
        if (pathVars.containsKey("linkId")) {
            resourceOwnershipService.assertPaymentLinkOwned(pathVars.get("linkId"), method, path, clientIp, userAgent);
        }
        return true;
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return comma > 0 ? forwarded.substring(0, comma).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }
}
