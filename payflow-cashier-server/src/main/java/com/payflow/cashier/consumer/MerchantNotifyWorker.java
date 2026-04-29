package com.payflow.cashier.consumer;

import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.config.PayflowProperties;
import com.payflow.cashier.dto.MqMessage;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.util.MerchantSignatureUtil;
import com.payflow.cashier.service.OrderMqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 商户异步通知 HTTP 投递与签名（供主消费与重试消费复用）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MerchantNotifyWorker {

    private final OrderMapper orderMapper;
    private final OrderMqProducer orderMqProducer;
    private final PayflowProperties payflowProperties;

    /**
     * 执行一次商户回调；失败且仍有重试次数时发送延迟重试 MQ。
     */
    public void deliverOrScheduleRetry(MqMessage message) {
        String orderId = message.getOrderId();
        try {
            Order order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));

            if (order == null) {
                log.warn("[商户回调] 订单不存在: orderId={}", orderId);
                return;
            }

            String merchantNotifyUrl = order.getMerchantNotifyUrl();
            if (merchantNotifyUrl == null || merchantNotifyUrl.isBlank()) {
                log.info("[商户回调] 商户未配置回调地址，跳过: orderId={}", orderId);
                return;
            }

            Map<String, Object> notifyParams = buildNotifyParams(order, message);
            appendCallbackSign(order.getMerchantId(), notifyParams);

            boolean success = sendMerchantNotify(merchantNotifyUrl, notifyParams);

            if (success) {
                log.info("[商户回调] 回调成功: orderId={}, url={}", orderId, merchantNotifyUrl);
                return;
            }

            if (message.hasRetries()) {
                MqMessage retryMsg = message.incrementRetry();
                long delaySeconds = computeRetryDelay(retryMsg.getRetryCount());
                log.warn("[商户回调] 回调失败，投递延迟重试: orderId={}, retryCount={}, delaySeconds={}",
                        orderId, retryMsg.getRetryCount(), delaySeconds);
                orderMqProducer.sendMerchantNotifyRetry(retryMsg, delaySeconds);
            } else {
                log.error("[商户回调] 回调失败，已达最大重试次数: orderId={}", orderId);
            }
        } catch (Exception e) {
            log.error("[商户回调] 处理异常: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    private Map<String, Object> buildNotifyParams(Order order, MqMessage message) {
        java.util.HashMap<String, Object> notifyParams = new java.util.HashMap<>();
        notifyParams.put("orderId", order.getOrderId());
        notifyParams.put("merchantId", order.getMerchantId());
        notifyParams.put("merchantOrderNo", order.getMerchantOrderNo());
        notifyParams.put("status", message.getExt1());
        notifyParams.put("amount", order.getAmount());
        notifyParams.put("payAmount", order.getPayAmount());
        notifyParams.put("currency", order.getCurrency());
        notifyParams.put("payTime", order.getPayTime() != null
                ? order.getPayTime().toString() : null);
        notifyParams.put("ext2", message.getExt2());
        if (message.getRefundId() != null) {
            notifyParams.put("refundId", message.getRefundId());
        }
        if (message.getRefundAmount() != null) {
            notifyParams.put("refundAmount", message.getRefundAmount());
        }
        notifyParams.put("notifyTimestamp", String.valueOf(System.currentTimeMillis() / 1000));
        return notifyParams;
    }

    /**
     * 对回调参数追加 sign：HMAC-SHA256(hex)，原文为按键名排序后的 key=value& 拼接串，密钥为商户 appSecret。
     */
    private void appendCallbackSign(String merchantId, Map<String, Object> notifyParams) {
        String appSecret = payflowProperties.getMerchantAppSecret(merchantId);
        if (appSecret == null || appSecret.isBlank()) {
            log.warn("[商户回调] 未配置商户密钥，跳过回调签名: merchantId={}", merchantId);
            return;
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        notifyParams.forEach((k, v) -> {
            if (v != null && !"sign".equals(k)) {
                sorted.put(k, String.valueOf(v));
            }
        });
        String plain = sorted.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));
        String sign = MerchantSignatureUtil.hmacSha256Hex(plain, appSecret);
        notifyParams.put("sign", sign);
    }

    private boolean sendMerchantNotify(String url, Map<String, Object> params) {
        try {
            String response = HttpUtil.createPost(url)
                    .form(params)
                    .timeout(10000)
                    .execute()
                    .body();
            log.info("[商户回调] 收到商户响应: orderId={}, response={}", params.get("orderId"), response);
            return response != null && (
                    response.contains("success")
                            || response.contains("SUCCESS")
                            || response.contains("ok")
                            || response.contains("OK")
            );
        } catch (Exception e) {
            log.warn("[商户回调] HTTP 请求失败: url={}, error={}", url, e.getMessage());
            return false;
        }
    }

    private long computeRetryDelay(int retryCount) {
        return switch (retryCount) {
            case 1 -> 60;
            case 2 -> 300;
            case 3 -> 900;
            default -> 60;
        };
    }
}
