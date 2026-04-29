package com.payflow.admin.service;

import com.payflow.admin.entity.cashier.Payment;
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
        return paymentMapper.selectById(paymentId);
    }
}
