package com.payflow.cashier.client;

import com.payflow.cashier.config.PayflowProperties;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 转发公网入驻请求到管理端内部接口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminOnboardingClient {

    public static final String HEADER_INTERNAL_TOKEN = "X-Payflow-Internal-Token";

    private final RestTemplate restTemplate;
    private final PayflowProperties payflowProperties;

    public Map<String, Object> submitApplication(Map<String, Object> body) {
        return post("/api/v1/internal/onboarding/applications", body);
    }

    public Map<String, Object> queryResult(Map<String, Object> body) {
        return post("/api/v1/internal/onboarding/result", body);
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        PayflowProperties.Admin admin = payflowProperties.getAdmin();
        if (admin == null || admin.getBaseUrl() == null || admin.getBaseUrl().isBlank()) {
            throw new IllegalStateException("未配置 payflow.admin.base-url");
        }
        if (admin.getInternalToken() == null || admin.getInternalToken().isBlank()) {
            throw new IllegalStateException("未配置 payflow.admin.internal-token");
        }
        String url = admin.getBaseUrl().replaceAll("/+$", "") + path;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HEADER_INTERNAL_TOKEN, admin.getInternalToken());
        try {
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    new ParameterizedTypeReference<>() {
                    });
            Map<String, Object> envelope = resp.getBody();
            if (envelope == null) {
                throw new IllegalStateException("管理端响应体为空");
            }
            Object code = envelope.get("code");
            int c = code instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(code));
            if (c != 0) {
                Object msg = envelope.get("message");
                String message = msg != null ? msg.toString() : "管理端业务错误";
                throw new BizException(c, message);
            }
            Object data = envelope.get("data");
            if (data instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> cast = (Map<String, Object>) map;
                return cast;
            }
            throw new IllegalStateException("管理端响应 data 格式异常");
        } catch (RestClientException ex) {
            log.warn("调用管理端入驻接口失败: path={}, message={}", path, ex.getMessage());
            throw new IllegalStateException("无法连接管理端，请稍后重试", ex);
        }
    }
}
