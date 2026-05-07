package com.payflow.cashier.webhook;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Webhook 投递占位：后续接入 merchant_webhook_endpoint 表与 HTTP 客户端重试。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatchService {

    /**
     * 记录待投递事件（当前仅打日志，避免引入未配置的外部 HTTP）。
     */
    public void publish(String merchantId, WebhookEventCode event, Map<String, Object> payload) {
        log.info("Webhook事件(占位): merchantId={}, event={}, keys={}", merchantId, event.getCode(), payload.keySet());
    }
}
