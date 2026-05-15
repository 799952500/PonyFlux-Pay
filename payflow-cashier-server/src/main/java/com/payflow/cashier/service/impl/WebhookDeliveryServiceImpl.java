package com.payflow.cashier.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.MerchantWebhookEndpoint;
import com.payflow.cashier.entity.WebhookDeliveryLog;
import com.payflow.cashier.mapper.MerchantWebhookEndpointMapper;
import com.payflow.cashier.mapper.WebhookDeliveryLogMapper;
import com.payflow.cashier.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

/**
 * Webhook 投递服务实现——HTTP POST + HMAC-SHA256 签名 + 投递日志持久化。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDeliveryServiceImpl implements WebhookDeliveryService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    private final WebhookDeliveryLogMapper deliveryLogMapper;
    private final MerchantWebhookEndpointMapper endpointMapper;

    @Override
    public void deliver(WebhookDeliveryLog deliveryLog) {
        try {
            MerchantWebhookEndpoint endpoint = endpointMapper.selectById(deliveryLog.getEndpointId());
            if (endpoint == null) {
                log.error("Webhook端点不存在: endpointId={}", deliveryLog.getEndpointId());
                markFailed(deliveryLog, "端点不存在");
                return;
            }

            // 构建签名
            long timestamp = System.currentTimeMillis() / 1000;
            String payload = deliveryLog.getPayloadJson();
            String sign = computeHmacSha256(endpoint.getSecret(), timestamp + "." + payload);

            // HTTP POST
            HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint.getUrl()).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("X-Payflow-Timestamp", String.valueOf(timestamp));
            conn.setRequestProperty("X-Payflow-Sign", sign);
            conn.setRequestProperty("X-Payflow-Event", deliveryLog.getEventCode());
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int httpStatus = conn.getResponseCode();
            String responseBody = readResponseBody(conn, httpStatus);

            deliveryLog.setHttpStatus(httpStatus);
            deliveryLog.setResponseBody(responseBody != null && responseBody.length() > 2048
                    ? responseBody.substring(0, 2048) : responseBody);

            if (httpStatus >= 200 && httpStatus < 300) {
                deliveryLog.setStatus(WebhookDeliveryLog.STATUS_SUCCESS);
                log.info("Webhook投递成功: merchantId={}, event={}, url={}, httpStatus={}",
                        deliveryLog.getMerchantId(), deliveryLog.getEventCode(), endpoint.getUrl(), httpStatus);
            } else {
                log.warn("Webhook投递失败: merchantId={}, event={}, url={}, httpStatus={}, response={}",
                        deliveryLog.getMerchantId(), deliveryLog.getEventCode(), endpoint.getUrl(), httpStatus, responseBody);
                handleRetryOrFail(deliveryLog);
            }
        } catch (Exception e) {
            log.error("Webhook投递异常: deliveryId={}, event={}", deliveryLog.getId(), deliveryLog.getEventCode(), e);
            deliveryLog.setResponseBody(e.getMessage() != null && e.getMessage().length() > 2048
                    ? e.getMessage().substring(0, 2048) : e.getMessage());
            handleRetryOrFail(deliveryLog);
        }
        deliveryLogMapper.updateById(deliveryLog);
    }

    @Override
    public boolean retry(WebhookDeliveryLog deliveryLog) {
        MerchantWebhookEndpoint endpoint = endpointMapper.selectById(deliveryLog.getEndpointId());
        if (endpoint == null || !Boolean.TRUE.equals(endpoint.getEnabled())) {
            markFailed(deliveryLog, "端点已禁用或不存在");
            deliveryLogMapper.updateById(deliveryLog);
            return false;
        }

        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String payload = deliveryLog.getPayloadJson();
            String sign = computeHmacSha256(endpoint.getSecret(), timestamp + "." + payload);

            HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint.getUrl()).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
            conn.setRequestProperty("X-Payflow-Timestamp", String.valueOf(timestamp));
            conn.setRequestProperty("X-Payflow-Sign", sign);
            conn.setRequestProperty("X-Payflow-Event", deliveryLog.getEventCode());
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }

            int httpStatus = conn.getResponseCode();
            deliveryLog.setHttpStatus(httpStatus);
            deliveryLog.setAttempt(deliveryLog.getAttempt() + 1);

            if (httpStatus >= 200 && httpStatus < 300) {
                deliveryLog.setStatus(WebhookDeliveryLog.STATUS_SUCCESS);
                deliveryLogMapper.updateById(deliveryLog);
                return true;
            } else {
                handleRetryOrFail(deliveryLog);
                deliveryLogMapper.updateById(deliveryLog);
                return false;
            }
        } catch (Exception e) {
            log.error("Webhook重试异常: deliveryId={}", deliveryLog.getId(), e);
            deliveryLog.setAttempt(deliveryLog.getAttempt() + 1);
            handleRetryOrFail(deliveryLog);
            deliveryLogMapper.updateById(deliveryLog);
            return false;
        }
    }

    @Override
    public List<WebhookDeliveryLog> findByEvent(String eventCode, String merchantId) {
        return deliveryLogMapper.selectList(
                new LambdaQueryWrapper<WebhookDeliveryLog>()
                        .eq(WebhookDeliveryLog::getEventCode, eventCode)
                        .eq(WebhookDeliveryLog::getMerchantId, merchantId)
                        .orderByDesc(WebhookDeliveryLog::getCreatedAt));
    }

    /**
     * 根据重试次数决定下次重试或标记失败（指数退避：60s * 2^attempt）。
     */
    private void handleRetryOrFail(WebhookDeliveryLog deliveryLog) {
        int nextAttempt = deliveryLog.getAttempt() + 1;
        deliveryLog.setAttempt(nextAttempt);
        if (nextAttempt >= WebhookDeliveryLog.MAX_RETRY) {
            deliveryLog.setStatus(WebhookDeliveryLog.STATUS_FAILED);
            log.error("Webhook投递最终失败: merchantId={}, event={}, attempts={}",
                    deliveryLog.getMerchantId(), deliveryLog.getEventCode(), nextAttempt);
        } else {
            deliveryLog.setStatus(WebhookDeliveryLog.STATUS_PENDING);
        }
    }

    private void markFailed(WebhookDeliveryLog deliveryLog, String reason) {
        deliveryLog.setStatus(WebhookDeliveryLog.STATUS_FAILED);
        deliveryLog.setResponseBody(reason);
    }

    private static String computeHmacSha256(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256签名失败", e);
        }
    }

    private static String readResponseBody(HttpURLConnection conn, int httpStatus) {
        try {
            java.io.InputStream is = httpStatus >= 200 && httpStatus < 400
                    ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return "";
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
