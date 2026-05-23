package com.payflow.admin.client;

import com.payflow.admin.config.CashierClientProperties;
import com.payflow.admin.dto.onboarding.MerchantProvisionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 审批通过后向收银台写入 cashier_merchants（内部 API）。
 */
@Component
@RequiredArgsConstructor
public class CashierMerchantProvisionClient {

    private static final String HEADER_INTERNAL_TOKEN = "X-Payflow-Internal-Token";

    private final RestTemplate restTemplate;
    private final CashierClientProperties cashierClientProperties;

    public void provision(MerchantProvisionRequest request) {
        if (!StringUtils.hasText(cashierClientProperties.getInternalToken())) {
            throw new IllegalStateException("未配置 payflow.cashier.internal-token，无法同步收银台商户");
        }
        String url = cashierClientProperties.getBaseUrl().replaceAll("/+$", "")
                + "/api/v1/internal/merchants/provision";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add(HEADER_INTERNAL_TOKEN, cashierClientProperties.getInternalToken());
        var resp = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(request, headers), Map.class);
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
