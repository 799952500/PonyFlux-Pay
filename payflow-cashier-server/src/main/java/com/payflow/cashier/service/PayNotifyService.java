package com.payflow.cashier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.payflow.cashier.context.MerchantScopeHolder;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.metrics.PaymentMetrics;
import com.payflow.cashier.routing.ChannelHealthRedisService;
import com.payflow.cashier.webhook.WebhookDispatchService;
import com.payflow.cashier.webhook.WebhookEventCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调通用业务逻辑服务。
 * <p>
 * 所有渠道的支付成功处理逻辑统一收敛在此处，避免代码重复。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayNotifyService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final ChannelHealthRedisService channelHealthRedisService;
    private final WebhookDispatchService webhookDispatchService;
    private final PaymentMetrics paymentMetrics;
    private final NotifyDedupService notifyDedupService;

    /**
     * 处理支付成功（更新 Payment + Order 状态）。
     *
     * @param orderId               商户订单号
     * @param channelTransactionId   渠道交易号
     */
    public void handlePaymentSuccess(String orderId, String channelTransactionId) {
        MerchantScopeHolder.runInSystemMode(() -> handlePaymentSuccessInternal(orderId, channelTransactionId));
    }

    private void handlePaymentSuccessInternal(String orderId, String channelTransactionId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .orderByDesc(Payment::getCreatedAt)
                        .last("LIMIT 1"));
        if (payment == null) {
            log.warn("未找到待支付记录: orderId={}", orderId);
            return;
        }

        if (Payment.STATUS_SUCCESS.equals(payment.getStatus())) {
            log.info("支付已成功，跳过重复回调: orderId={}, paymentId={}", orderId, payment.getPaymentId());
            return;
        }

        UpdateWrapper<Payment> update = new UpdateWrapper<>();
        update.eq("payment_id", payment.getPaymentId())
                .eq("status", Payment.STATUS_PROCESSING);
        Payment patch = new Payment();
        patch.setChannelTransactionId(channelTransactionId);
        patch.setStatus(Payment.STATUS_SUCCESS);
        patch.setUpdatedAt(LocalDateTime.now());
        int affected = paymentMapper.update(patch, update);
        if (affected == 0) {
            log.warn("支付状态更新未生效（可能并发或已处理）: orderId={}, paymentId={}", orderId, payment.getPaymentId());
            return;
        }

        if (payment.getAccountCode() != null) {
            channelHealthRedisService.recordOutcome(payment.getAccountCode(), true);
        }
        paymentMetrics.recordSuccess(payment.getPayChannel());

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
        if (order != null) {
            orderService.updateOrderStatus(orderId, Order.STATUS_PAID, payment.getAmount());
            if (notifyDedupService.tryMark(payment.getPaymentId(), "PAYMENT_SUCCESS")) {
                Map<String, Object> payload = new HashMap<>();
                payload.put("orderId", orderId);
                payload.put("paymentId", payment.getPaymentId());
                payload.put("channelTransactionId", channelTransactionId != null ? channelTransactionId : "");
                payload.put("amount", order.getAmount());
                payload.put("currency", order.getCurrency());
                payload.put("status", Order.STATUS_PAID);
                payload.put("paidAt", LocalDateTime.now().toString());
                webhookDispatchService.publish(order.getMerchantId(), WebhookEventCode.PAYMENT_SUCCESS, payload);
            } else {
                log.info("Webhook 去重跳过: paymentId={}", payment.getPaymentId());
            }
        }

        log.info("支付完成: orderId={}, paymentId={}", orderId, payment.getPaymentId());
    }
}
