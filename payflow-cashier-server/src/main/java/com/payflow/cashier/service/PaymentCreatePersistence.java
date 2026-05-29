package com.payflow.cashier.service;

import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.util.SignUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 支付下单落库（短事务），与渠道 HTTP 调用分离。
 */
@Service
@RequiredArgsConstructor
public class PaymentCreatePersistence {

    private final PaymentMapper paymentMapper;
    private final OrderService orderService;

    /**
     * 写入 PROCESSING 支付记录并将订单置为 PAYING。
     *
     * @return 新生成的 paymentId
     */
    @Transactional(rollbackFor = Exception.class)
    public String persistProcessingPayment(String orderId, String actualChannel, String payMethod,
                                           PayChannelAccount account, long amount) {
        String paymentId = SignUtils.generatePaymentId();
        LocalDateTime now = LocalDateTime.now();
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .payChannel(actualChannel)
                .accountCode(account.getAccountCode())
                .payMethod(payMethod)
                .amount(amount)
                .status(Payment.STATUS_PROCESSING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        paymentMapper.insert(payment);
        orderService.updateOrderStatus(orderId, Order.STATUS_PAYING, null);
        return paymentId;
    }
}
