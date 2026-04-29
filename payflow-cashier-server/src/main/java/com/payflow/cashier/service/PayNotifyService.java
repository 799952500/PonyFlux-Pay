package com.payflow.cashier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

    /**
     * 处理支付成功（更新 Payment + Order 状态）。
     *
     * @param orderId               商户订单号
     * @param channelTransactionId   渠道交易号
     */
    public void handlePaymentSuccess(String orderId, String channelTransactionId) {
        // 查询 Payment 记录
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getStatus, Payment.STATUS_PROCESSING)
                        .orderByDesc(Payment::getCreatedAt)
                        .last("LIMIT 1"));
        if (payment == null) {
            log.warn("未找到待支付记录: orderId={}", orderId);
            return;
        }

        // 更新 Payment
        payment.setChannelTransactionId(channelTransactionId);
        payment.setStatus(Payment.STATUS_SUCCESS);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 更新 Order
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
        if (order != null) {
            orderService.updateOrderStatus(orderId, Order.STATUS_PAID, null);
        }

        log.info("支付完成: orderId={}, paymentId={}", orderId, payment.getPaymentId());
    }
}
