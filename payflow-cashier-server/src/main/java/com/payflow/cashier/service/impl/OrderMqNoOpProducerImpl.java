package com.payflow.cashier.service.impl;

import com.payflow.cashier.service.OrderMqProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * MQ 生产者 NoOp 实现（无 MQ 环境降级用）
 * 所有消息操作直接跳过
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "payflow.mq.enabled", havingValue = "false", matchIfMissing = true)
public class OrderMqNoOpProducerImpl implements OrderMqProducer {

    public OrderMqNoOpProducerImpl() {
        log.warn("[MQ-Stub] OrderMqNoOpProducerImpl initialized — RocketMQ producer disabled (payflow.mq.enabled=false)");
    }

    @Override
    public void sendOrderTimeoutCheck(String orderId, LocalDateTime expireTime) {
        log.debug("[MQ-Stub] 跳过发送订单超时检查消息: orderId={}", orderId);
    }

    @Override
    public void sendPaymentResultNotify(String orderId, String status, String paymentId) {
        log.debug("[MQ-Stub] 跳过发送支付结果通知消息: orderId={}, status={}", orderId, status);
    }

    @Override
    public void sendRefundResultNotify(String orderId, String paymentStatus, String paymentId,
                                       String refundId, Long refundAmount) {
        log.debug("[MQ-Stub] 跳过发送退款结果通知消息: orderId={}, refundId={}", orderId, refundId);
    }

    @Override
    public void sendMerchantNotifyRetry(com.payflow.cashier.dto.MqMessage message, long delaySeconds) {
        log.debug("[MQ-Stub] 跳过发送商户回调重试消息: delaySeconds={}", delaySeconds);
    }
}
