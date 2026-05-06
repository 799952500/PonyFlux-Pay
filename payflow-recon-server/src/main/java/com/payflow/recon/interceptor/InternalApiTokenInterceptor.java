package com.payflow.recon.interceptor;

import com.payflow.recon.config.ReconProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 校验管理端等服务调用对账内部接口时的令牌。
 *
 * @author PayFlow Team
 */
@Component
@RequiredArgsConstructor
public class InternalApiTokenInterceptor implements HandlerInterceptor {

    public static final String HEADER_INTERNAL_TOKEN = "X-Payflow-Internal-Token";

    private final ReconProperties reconProperties;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        String expected = reconProperties.getInternalToken();
        if (expected == null || expected.isBlank()) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":503,\"message\":\"对账服务未配置内部调用令牌\"}");
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
