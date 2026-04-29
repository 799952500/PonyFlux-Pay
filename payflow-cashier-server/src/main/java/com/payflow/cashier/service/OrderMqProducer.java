package com.payflow.cashier.service;

import com.payflow.cashier.dto.MqMessage;

import java.time.LocalDateTime;

/**
 * 订单消息队列生产者接口
 *
 * @author PayFlow Team
 */
public interface OrderMqProducer {

    /**
     * 发送订单超时检查消息（延迟消息）
     *
     * @param orderId    订单号
     * @param expireTime 订单过期时间
     */
    void sendOrderTimeoutCheck(String orderId, LocalDateTime expireTime);

    /**
     * 发送支付结果通知商户消息
     *
     * @param orderId   订单号
     * @param status   支付状态
     * @param paymentId 支付渠道交易号
     */
    void sendPaymentResultNotify(String orderId, String status, String paymentId);

    /**
     * 发送退款结果通知商户消息（与支付结果共用投递与重试逻辑）
     *
     * @param orderId        订单号
     * @param paymentStatus  支付单状态（如 REFUNDED、PARTIAL_REFUND）
     * @param paymentId      支付单号
     * @param refundId       平台退款单号
     * @param refundAmount   本次退款金额（分）
     */
    void sendRefundResultNotify(String orderId, String paymentStatus, String paymentId,
                                String refundId, Long refundAmount);

    /**
     * 发送商户回调重试消息
     *
     * @param message 原始消息（包含重试次数）
     * @param delaySeconds 延迟秒数
     */
    void sendMerchantNotifyRetry(MqMessage message, long delaySeconds);
}