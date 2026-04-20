package com.payflow.cashier.service.impl;

import com.payflow.cashier.config.MqConfig;
import com.payflow.cashier.dto.MqMessage;
import com.payflow.cashier.service.OrderMqProducer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 订单 MQ 生产者实现
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(RocketMQTemplate.class)
public class OrderMqProducerImpl implements OrderMqProducer {

    private final RocketMQTemplate rocketMQTemplate;

    /** RocketMQ 延迟级别：18=30分钟 */
    private static final long DELAY_LEVEL_30MIN = 18;

    @Override
    public void sendOrderTimeoutCheck(String orderId, LocalDateTime expireTime) {
        try {
            MqMessage message = MqMessage.of(orderId);
            long delaySeconds = Duration.between(LocalDateTime.now(), expireTime).getSeconds();
            // 确保至少延迟1分钟，最长不超过RocketMQ支持的最大延迟（2小时）
            delaySeconds = Math.max(60, Math.min(delaySeconds, 7200));
            long delayLevel = computeDelayLevel(delaySeconds);

            // RocketMQTemplate.asyncSend(String, Message<?>, SendCallback, long) 
            // 注意：泛型擦除后 Message<MqMessage> → Message<?> 需要显式强转
            @SuppressWarnings("unchecked")
            Message<MqMessage> typedMessage = MessageBuilder.withPayload(message).build();
            Message<?> rawMessage = (Message<?>) (Message<?>) typedMessage;

            rocketMQTemplate.asyncSend(
                    MqConfig.TOPIC_ORDER_TIMEOUT + ":timeout",
                    rawMessage,
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("订单超时检查消息发送成功: orderId={}, msgId={}",
                                    orderId, sendResult.getMsgId());
                        }

                        @Override
                        public void onException(Throwable e) {
                            log.error("订单超时检查消息发送失败: orderId={}, error={}",
                                    orderId, e.getMessage());
                        }
                    },
                    3000L
            );
            log.info("发送订单超时检查消息: orderId={}, delayLevel={}", orderId, delayLevel);
        } catch (Exception e) {
            log.error("发送订单超时检查消息异常: orderId={}, error={}", orderId, e.getMessage(), e);
        }
    }

    @Override
    public void sendPaymentResultNotify(String orderId, String status, String paymentId) {
        try {
            MqMessage message = MqMessage.of(orderId, status, paymentId);

            @SuppressWarnings("unchecked")
            Message<MqMessage> typedMessage = MessageBuilder.withPayload(message).build();
            Message<?> rawMessage = (Message<?>) (Message<?>) typedMessage;

            rocketMQTemplate.asyncSend(
                    MqConfig.TOPIC_MERCHANT_NOTIFY + ":notify",
                    rawMessage,
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("支付结果通知消息发送成功: orderId={}, status={}, msgId={}",
                                    orderId, status, sendResult.getMsgId());
                        }

                        @Override
                        public void onException(Throwable e) {
                            log.error("支付结果通知消息发送失败: orderId={}, status={}, error={}",
                                    orderId, status, e.getMessage());
                        }
                    },
                    3000L
            );
            log.info("发送支付结果通知消息: orderId={}, status={}", orderId, status);
        } catch (Exception e) {
            log.error("发送支付结果通知消息异常: orderId={}, status={}, error={}",
                    orderId, status, e.getMessage(), e);
        }
    }

    @Override
    public void sendMerchantNotifyRetry(MqMessage message, long delaySeconds) {
        try {
            @SuppressWarnings("unchecked")
            Message<MqMessage> typedMessage = MessageBuilder.withPayload(message).build();
            Message<?> rawMessage = (Message<?>) (Message<?>) typedMessage;

            long delayLevel = computeDelayLevel(delaySeconds);
            rocketMQTemplate.asyncSend(
                    MqConfig.TOPIC_MERCHANT_NOTIFY + ":retry",
                    rawMessage,
                    new SendCallback() {
                        @Override
                        public void onSuccess(SendResult sendResult) {
                            log.info("商户回调重试消息发送成功: delaySeconds={}, msgId={}",
                                    delaySeconds, sendResult.getMsgId());
                        }

                        @Override
                        public void onException(Throwable e) {
                            log.error("商户回调重试消息发送失败: delaySeconds={}, error={}",
                                    delaySeconds, e.getMessage());
                        }
                    },
                    3000L
            );
            log.info("发送商户回调重试消息: delaySeconds={}, delayLevel={}", delaySeconds, delayLevel);
        } catch (Exception e) {
            log.error("发送商户回调重试消息异常: delaySeconds={}, error={}",
                    delaySeconds, e.getMessage(), e);
        }
    }

    /**
     * 根据秒数计算 RocketMQ 延迟级别
     * RocketMQ delayLevel: 1=1s, 2=5s, 3=10s, 4=30s, 5=1m, 6=2m, 7=3m, 8=4m, 9=5m,
     *                    10=10m, 11=20m, 12=30m, 13=1h, 14=2h
     */
    private long computeDelayLevel(long delaySeconds) {
        if (delaySeconds <= 1) return 1;
        if (delaySeconds <= 5) return 2;
        if (delaySeconds <= 10) return 3;
        if (delaySeconds <= 30) return 4;
        if (delaySeconds <= 60) return 5;
        if (delaySeconds <= 120) return 6;
        if (delaySeconds <= 180) return 7;
        if (delaySeconds <= 240) return 8;
        if (delaySeconds <= 300) return 9;
        if (delaySeconds <= 600) return 10;
        if (delaySeconds <= 1200) return 11;
        if (delaySeconds <= 1800) return 12;
        if (delaySeconds <= 3600) return 13;
        return 14; // 2h
    }
}
