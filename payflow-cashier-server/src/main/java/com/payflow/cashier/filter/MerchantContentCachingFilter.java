package com.payflow.cashier.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;

/**
 * 对商户 API 请求包装 ContentCachingRequestWrapper，供 merchantId 绑定校验读取 JSON 体。
 *
 * @author PayFlow Team
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class MerchantContentCachingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (shouldWrap(request)) {
            ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request);
            filterChain.doFilter(wrapped, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean shouldWrap(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }
        if (uri.startsWith("/api/v1/orders")
                || uri.startsWith("/api/v1/payments")
                || uri.startsWith("/api/v1/refunds")
                || uri.startsWith("/api/v1/merchant")
                || uri.startsWith("/api/v1/payment-links")) {
            String method = request.getMethod();
            return "POST".equalsIgnoreCase(method)
                    || "PUT".equalsIgnoreCase(method)
                    || "PATCH".equalsIgnoreCase(method);
        }
        return false;
    }
}
