package com.payflow.admin.client;

import com.payflow.admin.config.CashierClientProperties;
import com.payflow.admin.dto.AdminOrderRefundRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 收银台内部 API 客户端（退款执行、待审批退款、渠道查单）。
 */
@Component
@RequiredArgsConstructor
public class CashierInternalClient {

    private static final String HEADER_INTERNAL_TOKEN = "X-Payflow-Internal-Token";

    private final RestTemplate restTemplate;
    private final CashierClientProperties cashierClientProperties;

    public void executeRefund(String refundId) {
        String url = baseUrl() + "/api/v1/internal/refunds/" + refundId + "/execute";
        exchangePost(url, null);
    }

    public Map<String, Object> createPendingRefund(String orderId, AdminOrderRefundRequest body) {
        String url = baseUrl() + "/api/v1/internal/refunds/orders/" + orderId + "/pending";
        Map<String, Object> payload = new HashMap<>();
        payload.put("paymentId", body.getPaymentId());
        payload.put("refundAmount", body.getRefundAmount());
        payload.put("reason", body.getReason());
        return exchangePost(url, payload);
    }

    public Map<String, Object> queryPaymentChannel(String paymentId, boolean sync) {
        String url = baseUrl() + "/api/v1/internal/payments/" + paymentId
                + "/query-channel?sync=" + sync;
        return exchangePost(url, null);
    }

    private Map<String, Object> exchangePost(String url, Object body) {
        if (!StringUtils.hasText(cashierClientProperties.getInternalToken())) {
            throw new IllegalStateException("未配置 payflow.cashier.internal-token，无法调用收银台内部接口");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_INTERNAL_TOKEN, cashierClientProperties.getInternalToken());
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        HttpEntity<Object> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<>() {
                });
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("收银台返回 HTTP " + resp.getStatusCode().value());
        }
        Map<String, Object> respBody = resp.getBody();
        if (respBody == null) {
            throw new IllegalStateException("收银台响应体为空");
        }
        Object codeObj = respBody.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(codeObj));
        if (code != 0) {
            Object msg = respBody.get("message");
            throw new IllegalStateException(msg != null ? msg.toString() : "收银台业务错误 code=" + code);
        }
        Object data = respBody.get("data");
        if (data instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return typed;
        }
        return Map.of();
    }

    private String baseUrl() {
        return cashierClientProperties.getBaseUrl().replaceAll("/+$", "");
    }
}
