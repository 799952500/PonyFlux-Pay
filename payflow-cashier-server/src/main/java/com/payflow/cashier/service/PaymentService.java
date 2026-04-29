package com.payflow.cashier.service;

import com.payflow.cashier.dto.CreatePaymentRequest;
import com.payflow.cashier.dto.CreatePaymentResponse;

/**
 * 支付服务接口
 *
 * @author PayFlow Team
 */
public interface PaymentService {

    /**
     * 发起支付（商户签名验证后调用）
     * @param merchantId 已通过签名验证的商户ID
     * @param request    支付请求
     */
    CreatePaymentResponse createPayment(String merchantId, CreatePaymentRequest request);

    String getPaymentStatus(String paymentId);

    /**
     * 商户查询支付状态（校验支付关联订单归属商户）。
     */
    String getPaymentStatusForMerchant(String merchantId, String paymentId);
}
