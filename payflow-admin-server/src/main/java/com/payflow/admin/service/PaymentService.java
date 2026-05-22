package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.entity.cashier.Payment;
import com.payflow.admin.mapper.cashier.OrderMapper;
import com.payflow.admin.mapper.cashier.PaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 支付记录服务（查询 cashier 库）
  * @author Lucas
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    /**
     * 按订单号查询支付记录
     */
    public List<Payment> findByOrderId(String orderId) {
        return paymentMapper.findByOrderId(orderId);
    }

    /**
     * 按渠道查询支付记录
     */
    public List<Payment> findByPayChannel(String payChannel) {
        return paymentMapper.findByPayChannel(payChannel);
    }

    /**
     * 根据支付ID查询
     */
    public Payment getByPaymentId(String paymentId) {
        return paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentId, paymentId));
    }

    public Payment getByPaymentId(String paymentId, List<String> merchantScopeIds) {
        Payment payment = getByPaymentId(paymentId);
        if (payment == null || isPaymentVisible(payment, merchantScopeIds)) {
            return payment;
        }
        return null;
    }

    private boolean isPaymentVisible(Payment payment, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return true;
        }
        if (merchantScopeIds.isEmpty() || payment.getOrderId() == null) {
            return false;
        }
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderId, payment.getOrderId())
                .last("LIMIT 1"));
        return order != null && order.getMerchantId() != null && merchantScopeIds.contains(order.getMerchantId());
    }
}
