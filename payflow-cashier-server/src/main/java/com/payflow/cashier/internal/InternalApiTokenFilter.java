package com.payflow.cashier.internal;

import com.payflow.cashier.config.InternalApiProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 校验 {@code /api/v1/internal/**} 请求的内部令牌请求头。
 *
 * @author Lucas
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class InternalApiTokenFilter extends OncePerRequestFilter {

    public static final String HEADER_INTERNAL_TOKEN = "X-Payflow-Internal-Token";

    private final InternalApiProperties internalApiProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith("/api/v1/internal");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String expected = internalApiProperties.getToken();
        if (!StringUtils.hasText(expected)) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"code\":503,\"message\":\"internal API token not configured\"}");
            return;
        }
        String provided = request.getHeader(HEADER_INTERNAL_TOKEN);
        if (!expected.equals(provided)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"code\":403,\"message\":\"invalid internal token\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
