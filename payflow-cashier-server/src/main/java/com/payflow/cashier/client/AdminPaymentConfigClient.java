package com.payflow.cashier.client;

import com.payflow.cashier.config.PayflowProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 调用管理端内部接口，读取商户支付方式与终端范围。
 *
 * @author Lucas
 */
@Slf4j
@Component
public class AdminPaymentConfigClient {

    /** 与管理端 {@code InternalApiTokenInterceptor} 请求头一致 */
    public static final String HEADER_INTERNAL_TOKEN = "X-Payflow-Internal-Token";

    private final RestTemplate restTemplate;
    private final PayflowProperties payflowProperties;

    public AdminPaymentConfigClient(RestTemplate restTemplate, PayflowProperties payflowProperties) {
        this.restTemplate = restTemplate;
        this.payflowProperties = payflowProperties;
    }

    /**
     * @return 管理端返回的列表；调用失败或未配置 baseUrl 时返回空列表
     */
    public List<AdminPaymentMethodItem> fetchPaymentMethods(String merchantId, String orderChannel) {
        PayflowProperties.Admin admin = payflowProperties.getAdmin();
        if (admin == null || admin.getBaseUrl() == null || admin.getBaseUrl().isBlank()) {
            return List.of();
        }
        if (merchantId == null || merchantId.isBlank() || orderChannel == null || orderChannel.isBlank()) {
            return List.of();
        }
        String url = UriComponentsBuilder.fromUriString(admin.getBaseUrl().trim())
                .path("/api/v1/internal/cashier/payment-methods")
                .queryParam("merchantId", merchantId)
                .queryParam("orderChannel", orderChannel)
                .build(true)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        if (admin.getInternalToken() != null && !admin.getInternalToken().isBlank()) {
            headers.set(HEADER_INTERNAL_TOKEN, admin.getInternalToken());
        }
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> body = resp.getBody();
            if (body == null || !Integer.valueOf(0).equals(asInt(body.get("code")))) {
                return List.of();
            }
            Object data = body.get("data");
            if (!(data instanceof List<?> rawList)) {
                return List.of();
            }
            return rawList.stream()
                    .filter(Map.class::isInstance)
                    .map(o -> (Map<?, ?>) o)
                    .map(this::mapRow)
                    .toList();
        } catch (RestClientException ex) {
            log.warn("拉取管理端支付方式配置失败: merchantId={}, orderChannel={}, message={}",
                    merchantId, orderChannel, ex.getMessage());
            return List.of();
        }
    }

    private AdminPaymentMethodItem mapRow(Map<?, ?> row) {
        String methodCode = row.get("methodCode") != null ? row.get("methodCode").toString() : "";
        String methodName = row.get("methodName") != null ? row.get("methodName").toString() : "";
        String description = row.get("description") != null ? row.get("description").toString() : "";
        int priority = 0;
        if (row.get("priority") instanceof Number n) {
            priority = n.intValue();
        }
        Object scopesObj = row.get("clientScopes");
        List<String> scopes;
        if (scopesObj instanceof List<?> sl) {
            scopes = sl.stream().map(Object::toString).map(String::trim).map(String::toUpperCase).toList();
        } else {
            scopes = Collections.emptyList();
        }
        return new AdminPaymentMethodItem(methodCode, methodName, description, priority, scopes);
    }

    private static Integer asInt(Object o) {
        if (o instanceof Number n) {
            return n.intValue();
        }
        return null;
    }

    /**
     * 管理端单项支付方式说明。
     */
    public record AdminPaymentMethodItem(
            String methodCode,
            String methodName,
            String description,
            int priority,
            List<String> clientScopes
    ) {
        /**
         * 是否在给定终端下可见；终端为空表示不过滤。
         */
        public boolean visibleForClient(String normalizedClient) {
            if (normalizedClient == null || normalizedClient.isBlank()) {
                return true;
            }
            if (clientScopes == null || clientScopes.isEmpty()) {
                return true;
            }
            return clientScopes.contains(normalizedClient);
        }
    }
}
