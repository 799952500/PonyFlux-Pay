package com.payflow.admin.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

/**
 * 全链路追踪过滤器：从请求头提取或生成 Trace ID，设置到 MDC 和响应头。
 *
 * <p>上游传递的 Trace ID 通过请求头 {@code X-Trace-Id} 接收，
 * 不存在时自动生成 UUID。所有日志消息自动包含 Trace ID。</p>
 *
 * @author PayFlow Team
 */
public class TraceFilter implements Filter {

    /** Trace ID 在 MDC 中的 key */
    public static final String TRACE_ID_KEY = "traceId";

    /** 请求/响应头名称 */
    public static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String traceId = httpRequest.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        MDC.put(TRACE_ID_KEY, traceId);
        httpResponse.setHeader(TRACE_HEADER, traceId);

        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
