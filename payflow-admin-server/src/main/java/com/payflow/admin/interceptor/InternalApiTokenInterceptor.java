package com.payflow.admin.interceptor;

import com.payflow.admin.config.CashierClientProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 校验收银台等服务调用管理端内部接口时的令牌。
 *
 * @author Lucas
 */
@Component
@RequiredArgsConstructor
public class InternalApiTokenInterceptor implements HandlerInterceptor {

    public static final String HEADER_INTERNAL_TOKEN = "X-Payflow-Internal-Token";

    private final CashierClientProperties cashierClientProperties;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String expected = cashierClientProperties.getInternalToken();
        if (expected == null || expected.isBlank()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":503,\"message\":\"管理端未配置内部调用令牌\"}");
            return false;
        }
        String token = request.getHeader(HEADER_INTERNAL_TOKEN);
        if (!expected.equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":401,\"message\":\"内部令牌无效\"}");
            return false;
        }
        return true;
    }
}
