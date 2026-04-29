package com.payflow.cashier.consumer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.cashier.config.MqConfig;
import com.payflow.cashier.dto.MqMessage;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.service.OrderCacheService;
import com.payflow.cashier.service.OrderMqProducer;
import com.payflow.cashier.service.OrderService;
import com.payflow.cashier.service.PayChannelService;
import com.payflow.payment.wechat.WxPayNativeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单 MQ 消费者：订单超时、商户回调及重试。
  * @author Lucas
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "payflow.mq.enabled", havingValue = "true", matchIfMissing = true)
public class OrderMqConsumer {

    public OrderMqConsumer() {
    }

    /**
     * 订单超时处理
     */
    @Component
    @RocketMQMessageListener(
            topic = MqConfig.TOPIC_ORDER_TIMEOUT,
            consumerGroup = MqConfig.CG_ORDER_TIMEOUT,
            consumeMode = ConsumeMode.ORDERLY,
            maxReconsumeTimes = 3
    )
    @RequiredArgsConstructor
    public static class OrderTimeoutConsumer implements RocketMQListener<String> {

        private final OrderMapper orderMapper;
        private final PaymentMapper paymentMapper;
        private final OrderCacheService orderCacheService;
        private final OrderService orderService;
        private final ObjectMapper objectMapper;
        private final PayChannelService payChannelService;
        private final WxPayNativeHandler wxPayNativeHandler;

        @Override
        public void onMessage(String messageBody) {
            MqMessage message;
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
                Order order = orderMapper.selectOne(
                        new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));

                if (order == null) {
                    log.warn("[订单超时] 订单不存在: orderId={}", orderId);
                    return;
                }

                if (Order.STATUS_CREATED.equals(order.getStatus())
                        || Order.STATUS_PAYING.equals(order.getStatus())
                        || Order.STATUS_PENDING.equals(order.getStatus())) {

                    if (order.getExpireTime() != null
                            && LocalDateTime.now().isAfter(order.getExpireTime())) {

                        log.info("[订单超时] 订单已过期，执行关单前查渠道: orderId={}", orderId);

                        boolean realPaid = checkPaymentWithChannel(orderId, order);

                        if (!realPaid) {
                            orderService.updateOrderStatus(orderId, Order.STATUS_CLOSED, null);
                            orderCacheService.evictOrder(orderId);
                            log.info("[订单超时] 订单已关单: orderId={}", orderId);
                        } else {
                            log.info("[订单超时] 订单实际已支付，忽略关单: orderId={}", orderId);
                        }
                    }
                } else {
                    log.info("[订单超时] 订单已非待支付状态，忽略: orderId={}, status={}",
                            orderId, order.getStatus());
                }
            } catch (Exception e) {
                log.error("[订单超时] 处理异常: orderId={}, error={}", orderId, e.getMessage(), e);
            }
        }

        /**
         * 本地已成功、或微信支付侧已成功，则视为已支付。
         */
        private boolean checkPaymentWithChannel(String orderId, Order order) {
            Payment paidLocal = paymentMapper.selectOne(
                    new LambdaQueryWrapper<Payment>()
                            .eq(Payment::getOrderId, orderId)
                            .eq(Payment::getStatus, Payment.STATUS_SUCCESS)
                            .last("LIMIT 1"));
            if (paidLocal != null) {
                return true;
            }

            Payment processing = paymentMapper.selectOne(
                    new LambdaQueryWrapper<Payment>()
                            .eq(Payment::getOrderId, orderId)
                            .eq(Payment::getStatus, Payment.STATUS_PROCESSING)
                            .orderByDesc(Payment::getCreatedAt)
                            .last("LIMIT 1"));
            if (processing == null) {
                return false;
            }
            if (!"WECHAT_PAY".equals(processing.getPayChannel())) {
                log.debug("[订单超时] 非微信渠道，跳过远程查单: orderId={}, channel={}", orderId,
                        processing.getPayChannel());
                return false;
            }
            PayChannelAccount account = payChannelService.routeToAccount(
                    order.getMerchantId(), processing.getPayChannel());
            if (account == null) {
                log.warn("[订单超时] 无可用账户，无法查单: orderId={}", orderId);
                return false;
            }
            return wxPayNativeHandler.queryOutTradeNoSuccess(orderId, account);
        }
    }

    /**
     * 商户回调（首次投递）
     */
    @Component
    @RocketMQMessageListener(
            topic = MqConfig.TOPIC_MERCHANT_NOTIFY,
            consumerGroup = MqConfig.CG_MERCHANT_NOTIFY,
            selectorExpression = "notify",
            consumeMode = ConsumeMode.ORDERLY,
            maxReconsumeTimes = 0
    )
    @RequiredArgsConstructor
    public static class MerchantNotifyConsumer implements RocketMQListener<String> {

        private final ObjectMapper objectMapper;
        private final MerchantNotifyWorker merchantNotifyWorker;

        @Override
        public void onMessage(String messageBody) {
            try {
                MqMessage message = objectMapper.readValue(messageBody, MqMessage.class);
                log.info("[商户回调] 收到回调消息: orderId={}, retryCount={}",
                        message.getOrderId(), message.getRetryCount());
                merchantNotifyWorker.deliverOrScheduleRetry(message);
            } catch (Exception e) {
                log.error("[商户回调] 解析消息失败: {}", messageBody, e);
            }
        }
    }

    /**
     * 商户回调延迟重试
     */
    @Component
    @RocketMQMessageListener(
            topic = MqConfig.TOPIC_MERCHANT_NOTIFY,
            consumerGroup = MqConfig.CG_MERCHANT_NOTIFY_RETRY,
            selectorExpression = "retry",
            consumeMode = ConsumeMode.ORDERLY,
            maxReconsumeTimes = 3
    )
    @RequiredArgsConstructor
    public static class MerchantNotifyRetryConsumer implements RocketMQListener<String> {

        private final ObjectMapper objectMapper;
        private final MerchantNotifyWorker merchantNotifyWorker;

        @Override
        public void onMessage(String messageBody) {
            try {
                MqMessage message = objectMapper.readValue(messageBody, MqMessage.class);
                log.info("[商户回调重试] 收到消息: orderId={}, retryCount={}",
                        message.getOrderId(), message.getRetryCount());
                merchantNotifyWorker.deliverOrScheduleRetry(message);
            } catch (Exception e) {
                log.error("[商户回调重试] 解析消息失败: {}", messageBody, e);
            }
        }
    }
}
