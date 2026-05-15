package com.payflow.admin.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 安全响应头过滤器，为所有 HTTP 响应添加安全相关的响应头。
 *
 * <p>包括：</p>
 * <ul>
 *   <li>{@code X-Content-Type-Options: nosniff} —— 禁止 MIME 类型嗅探</li>
 *   <li>{@code X-Frame-Options: DENY} —— 禁止页面被嵌入 iframe</li>
 *   <li>{@code Strict-Transport-Security} —— 仅 HTTPS 环境下启用 HSTS</li>
 * </ul>
 *
 * @author PayFlow Team
 */
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 仅 HTTPS 环境下启用 HSTS
        if (request.isSecure()) {
            httpResponse.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains");
        }

        chain.doFilter(request, response);
    }
}
