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
     * 发送订单超时检查消息（创建订单后延迟发送，例如30分钟）
     *
     * @param orderId    订单号
     * @param expireTime 订单过期时间
     */
    void sendOrderTimeoutCheck(String orderId, LocalDateTime expireTime);

    /**
     * 发送支付结果通知商户消息
     *
     * @param orderId  订单号
     * @param status   支付状态
     * @param paymentId 支付渠道交易号
     */
    void sendPaymentResultNotify(String orderId, String status, String paymentId);
}
