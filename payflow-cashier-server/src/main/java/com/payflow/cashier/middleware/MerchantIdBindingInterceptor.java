package com.payflow.cashier.middleware;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.security.MerchantIdGuard;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.nio.charset.StandardCharsets;

/**
 * 校验 query/JSON 中的 merchantId 与认证上下文一致。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantIdBindingInterceptor implements HandlerInterceptor {

    private final MerchantIdGuard merchantIdGuard;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String queryMerchantId = request.getParameter("merchantId");
        merchantIdGuard.assertMatchesContext(
                queryMerchantId,
                request.getMethod(),
                request.getRequestURI(),
                resolveClientIp(request),
                request.getHeader("User-Agent"));

        String bodyMerchantId = extractMerchantIdFromBody(request);
        merchantIdGuard.assertMatchesContext(
                bodyMerchantId,
                request.getMethod(),
                request.getRequestURI(),
                resolveClientIp(request),
                request.getHeader("User-Agent"));
        return true;
    }

    private String extractMerchantIdFromBody(HttpServletRequest request) {
        if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
            return null;
        }
        byte[] content = wrapper.getContentAsByteArray();
        if (content == null || content.length == 0) {
            return null;
        }
        String contentType = request.getContentType();
        if (contentType == null || !contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(new String(content, StandardCharsets.UTF_8));
            JsonNode node = root.get("merchantId");
            if (node == null || node.isNull()) {
                return null;
            }
            return node.asText();
        } catch (Exception e) {
            log.debug("解析请求体 merchantId 失败: {}", e.getMessage());
            return null;
        }
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
