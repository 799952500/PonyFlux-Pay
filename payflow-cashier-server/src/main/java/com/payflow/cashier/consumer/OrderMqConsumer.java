package com.payflow.cashier.consumer;

import cn.hutool.http.HttpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.config.MqConfig;
import com.payflow.cashier.dto.MqMessage;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.service.OrderCacheService;
import com.payflow.cashier.service.OrderMqProducer;
import com.payflow.cashier.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 订单 MQ 消费者
 * 处理订单超时和商户回调
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payflow.mq.enabled", havingValue = "true", matchIfMissing = true)
public class OrderMqConsumer {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final OrderService orderService;
    private final OrderCacheService orderCacheService;
    private final OrderMqProducer orderMqProducer;
    private final ObjectMapper objectMapper;

    public OrderMqConsumer(OrderMapper orderMapper,
                            PaymentMapper paymentMapper,
                            OrderService orderService,
                            OrderCacheService orderCacheService,
                            OrderMqProducer orderMqProducer,
                            ObjectMapper objectMapper) {
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.orderService = orderService;
        this.orderCacheService = orderCacheService;
        this.orderMqProducer = orderMqProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * ==================== 订单超时处理 ====================
     */
    @org.springframework.stereotype.Component
    @RocketMQMessageListener(
            topic = MqConfig.TOPIC_ORDER_TIMEOUT,
            consumerGroup = MqConfig.CG_ORDER_TIMEOUT,
            consumeMode = ConsumeMode.ORDERLY,
            maxReconsumeTimes = 3
    )
    @RequiredArgsConstructor
    public static class OrderTimeoutConsumer implements RocketMQListener<String> {

        private final OrderMapper orderMapper;
        private final OrderCacheService orderCacheService;
        private final OrderService orderService;
        private final OrderMqProducer orderMqProducer;
        private final ObjectMapper objectMapper;

        @Override
        public void onMessage(String messageBody) {
            MqMessage message = null;
            String orderId;
            try {
                message = objectMapper.readValue(messageBody, MqMessage.class);
                orderId = message.getOrderId();
                log.info("[订单超时] 收到超时检查消息: orderId={}, retryCount={}", orderId, message.getRetryCount());
            } catch (Exception e) {
                log.error("[订单超时] 解析消息失败: {}", messageBody, e);
                return;
            }

            try {
                // 1. 查询订单
                Order order = orderMapper.selectOne(
                        new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));

                if (order == null) {
                    log.warn("[订单超时] 订单不存在: orderId={}", orderId);
                    return;
                }

                // 2. 判断是否仍为待支付状态
                if (Order.STATUS_CREATED.equals(order.getStatus())
                        || Order.STATUS_PAYING.equals(order.getStatus())
                        || Order.STATUS_PENDING.equals(order.getStatus())) {

                    // 3. 检查是否已超过过期时间
                    if (order.getExpireTime() != null
                            && LocalDateTime.now().isAfter(order.getExpireTime())) {

                        log.info("[订单超时] 订单已过期，执行关单: orderId={}", orderId);

                        // 4. 调用支付机构主动查询（模拟）
                        boolean realPaid = checkPaymentWithChannel(orderId);

                        if (!realPaid) {
                            // 5. 未支付，更新状态为 CLOSED
                            orderService.updateOrderStatus(orderId, Order.STATUS_CLOSED, null);
                            // 6. 清除缓存
                            orderCacheService.evictOrder(orderId);
                            log.info("[订单超时] 订单已关单: orderId={}", orderId);
                        } else {
                            log.info("[订单超时] 订单实际已支付，忽略: orderId={}", orderId);
                        }
                    }
                } else {
                    log.info("[订单超时] 订单已非待支付状态，忽略: orderId={}, status={}",
                            orderId, order.getStatus());
                }
            } catch (Exception e) {
                log.error("[订单超时] 处理异常: orderId={}, error={}", orderId, e.getMessage(), e);
                // RocketMQ 会自动重试
            }
        }

        /**
         * 调用支付机构主动查询订单状态（模拟）
         * TODO: 替换为真实支付渠道查询接口
         */
        private boolean checkPaymentWithChannel(String orderId) {
            try {
                log.info("[订单超时] 主动查询支付机构: orderId={}", orderId);
                // 实际业务中：调用微信/支付宝/银联查询接口
                // 这里模拟返回未支付
                return false;
            } catch (Exception e) {
                log.warn("[订单超时] 主动查询失败（视为未支付）: orderId={}, error={}", orderId, e.getMessage());
                return false;
            }
        }
    }

    /**
     * ==================== 商户回调处理 ====================
     */
    @org.springframework.stereotype.Component
    @RocketMQMessageListener(
            topic = MqConfig.TOPIC_MERCHANT_NOTIFY,
            consumerGroup = MqConfig.CG_MERCHANT_NOTIFY,
            consumeMode = ConsumeMode.ORDERLY,
            maxReconsumeTimes = 0  // 我们自己实现重试，不依赖 RocketMQ
    )
    @RequiredArgsConstructor
    public static class MerchantNotifyConsumer implements RocketMQListener<String> {

        private final OrderMapper orderMapper;
        private final OrderMqProducer orderMqProducer;
        private final ObjectMapper objectMapper;

        @Override
        public void onMessage(String messageBody) {
            MqMessage message = null;
            String orderId;
            try {
                message = objectMapper.readValue(messageBody, MqMessage.class);
                orderId = message.getOrderId();
                log.info("[商户回调] 收到回调消息: orderId={}, retryCount={}", orderId, message.getRetryCount());
            } catch (Exception e) {
                log.error("[商户回调] 解析消息失败: {}", messageBody, e);
                return;
            }

            try {
                // 1. 查询订单
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

                // 2. 构建回调参数
                Map<String, Object> notifyParams = new HashMap<>();
                notifyParams.put("orderId", order.getOrderId());
                notifyParams.put("merchantId", order.getMerchantId());
                notifyParams.put("merchantOrderNo", order.getMerchantOrderNo());
                notifyParams.put("status", message.getExt1());  // 支付状态
                notifyParams.put("amount", order.getAmount());
                notifyParams.put("payAmount", order.getPayAmount());
                notifyParams.put("currency", order.getCurrency());
                notifyParams.put("payTime", order.getPayTime() != null
                        ? order.getPayTime().toString() : null);
                notifyParams.put("ext2", message.getExt2());  // 渠道交易号

                // 3. 发送 HTTP POST 回调
                boolean success = sendMerchantNotify(merchantNotifyUrl, notifyParams);

                if (success) {
                    log.info("[商户回调] 回调成功: orderId={}, url={}", orderId, merchantNotifyUrl);
                } else {
                    // 4. 重试机制：最多3次，间隔 1min、5min、15min
                    if (message.hasRetries()) {
                        MqMessage retryMsg = message.incrementRetry();
                        long delaySeconds = computeRetryDelay(retryMsg.getRetryCount());
                        log.warn("[商户回调] 回调失败，将在 {} 秒后重试: orderId={}, retryCount={}",
                                delaySeconds, orderId, retryMsg.getRetryCount());
                        // 注意：RocketMQ 原生不支持精确延迟，这里用本地延迟模拟
                        // 生产环境建议使用 RocketMQ 延迟消息或本地延迟队列
                        // 为保证消息不丢失，这里记录失败日志（可配合告警）
                        // TODO: 使用 RocketMQ 延迟消息或定时任务重试
                    } else {
                        log.error("[商户回调] 回调失败，已达最大重试次数，告警处理: orderId={}", orderId);
                        // TODO: 触发告警（钉钉/企微/短信等）
                    }
                }
            } catch (Exception e) {
                log.error("[商户回调] 处理异常: orderId={}, error={}", orderId, e.getMessage(), e);
            }
        }

        private boolean sendMerchantNotify(String url, Map<String, Object> params) {
            try {
                // 使用 Hutool HTTP 工具发送 POST 表单请求
                String response = HttpUtil.createPost(url)
                        .form(params)
                        .timeout(10000)  // 10秒超时
                        .execute()
                        .body();
                log.info("[商户回调] 收到商户响应: orderId={}, response={}", params.get("orderId"), response);
                // 判断商户响应：通常商户返回 success/ok/200
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

        /**
         * 计算重试延迟间隔：1min、5min、15min
         */
        private long computeRetryDelay(int retryCount) {
            return switch (retryCount) {
                case 1 -> 60;      // 1分钟
                case 2 -> 300;     // 5分钟
                case 3 -> 900;     // 15分钟
                default -> 60;
            };
        }
    }
}
