package com.payflow.cashier.service;

import com.payflow.cashier.dto.CreatePaymentRequest;
import com.payflow.cashier.dto.CreatePaymentResponse;

/**
 * 支付服务接口
 *
 * @author PayFlow Team
 */
public interface PaymentService {

    CreatePaymentResponse createPayment(CreatePaymentRequest request);

    String getPaymentStatus(String paymentId);
}
