package com.payflow.admin.client;

import com.payflow.admin.config.CashierClientProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 触发收银台执行渠道退款（内部 API）。
 *
 * @author Lucas
 */
@Component
@RequiredArgsConstructor
public class CashierInternalRefundClient {

    /** 与收银台 {@code InternalApiTokenFilter} 约定一致 */
    private static final String HEADER_INTERNAL_TOKEN = "X-Payflow-Internal-Token";

    private final RestTemplate restTemplate;
    private final CashierClientProperties cashierClientProperties;

    /**
     * 对已处于 REFUNDING 的退款单执行渠道退款。
     *
     * @param refundId 退款单号
     */
    public void executeRefund(String refundId) {
        if (!StringUtils.hasText(cashierClientProperties.getInternalToken())) {
            throw new IllegalStateException("未配置 payflow.cashier.internal-token，无法调用收银台内部退款接口");
        }
        String url = cashierClientProperties.getBaseUrl().replaceAll("/+$", "")
                + "/api/v1/internal/refunds/" + refundId + "/execute";
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_INTERNAL_TOKEN, cashierClientProperties.getInternalToken());
        ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(headers), Map.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("收银台返回 HTTP " + resp.getStatusCode().value());
        }
        Map<?, ?> body = resp.getBody();
        if (body == null) {
            throw new IllegalStateException("收银台响应体为空");
        }
        Object codeObj = body.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(codeObj));
        if (code != 0) {
            Object msg = body.get("message");
            throw new IllegalStateException(msg != null ? msg.toString() : "收银台业务错误 code=" + code);
        }
    }
}
