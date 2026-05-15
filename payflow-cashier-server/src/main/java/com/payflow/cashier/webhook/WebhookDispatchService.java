package com.payflow.cashier.webhook;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.MerchantWebhookEndpoint;
import com.payflow.cashier.entity.WebhookDeliveryLog;
import com.payflow.cashier.mapper.MerchantWebhookEndpointMapper;
import com.payflow.cashier.mapper.WebhookDeliveryLogMapper;
import com.payflow.cashier.service.WebhookDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Webhook 事件分发——查询商户订阅端点，创建投递日志，异步投递。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookDispatchService {

    private final MerchantWebhookEndpointMapper endpointMapper;
    private final WebhookDeliveryLogMapper deliveryLogMapper;
    private final WebhookDeliveryService deliveryService;

    /**
     * 发布事件：查询商户的已启用端点，创建投递日志，异步执行 HTTP POST。
     *
     * @param merchantId 商户号
     * @param event      事件代码
     * @param payload    事件负载
     */
    public void publish(String merchantId, WebhookEventCode event, Map<String, Object> payload) {
        if (merchantId == null || merchantId.isBlank()) {
            log.warn("Webhook跳过：merchantId为空, event={}", event.getCode());
            return;
        }

        // 查询商户已启用的 Webhook 端点
        List<MerchantWebhookEndpoint> endpoints = endpointMapper.selectList(
                new LambdaQueryWrapper<MerchantWebhookEndpoint>()
                        .eq(MerchantWebhookEndpoint::getMerchantId, merchantId)
                        .eq(MerchantWebhookEndpoint::getEnabled, true));
        if (endpoints.isEmpty()) {
            log.debug("商户未配置启用的Webhook端点: merchantId={}", merchantId);
            return;
        }

        String payloadJson = JSONUtil.toJsonStr(payload);

        for (MerchantWebhookEndpoint ep : endpoints) {
            // 检查端点是否订阅了该事件
            if (!isSubscribed(ep.getEventCodes(), event.getCode())) {
                continue;
            }

            // 创建投递日志
            WebhookDeliveryLog logEntry = new WebhookDeliveryLog();
            logEntry.setMerchantId(merchantId);
            logEntry.setEndpointId(ep.getId());
            logEntry.setEventCode(event.getCode());
            logEntry.setPayloadJson(payloadJson);
            logEntry.setAttempt(0);
            logEntry.setStatus(WebhookDeliveryLog.STATUS_PENDING);
            logEntry.setCreatedAt(LocalDateTime.now());
            deliveryLogMapper.insert(logEntry);

            // 异步投递
            dispatchAsync(logEntry);
        }
    }

    @Async
    public void dispatchAsync(WebhookDeliveryLog deliveryLog) {
        try {
            deliveryService.deliver(deliveryLog);
        } catch (Exception e) {
            log.error("Webhook异步投递异常: deliveryId={}", deliveryLog.getId(), e);
        }
    }

    /**
     * 检查端点是否订阅了指定事件（eventCodes 为逗号分隔的事件代码列表）。
     */
    private boolean isSubscribed(String eventCodes, String eventCode) {
        if (eventCodes == null || eventCodes.isBlank()) {
            return true; // 未配置则默认订阅全部
        }
        return Arrays.stream(eventCodes.split(","))
                .map(String::trim)
                .anyMatch(code -> code.equals(eventCode));
    }
}
