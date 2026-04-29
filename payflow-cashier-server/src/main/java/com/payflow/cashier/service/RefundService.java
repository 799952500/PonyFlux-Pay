package com.payflow.cashier.service;

import com.payflow.cashier.dto.RefundRequest;
import com.payflow.cashier.dto.RefundResponse;

/**
 * 退款服务接口。
  * @author Lucas
 */
public interface RefundService {

    /**
     * 申请退款（须已通过商户签名验证的 merchantId）。
     */
    RefundResponse refund(String merchantId, RefundRequest request);

    /**
     * 查询退款记录。
     */
    RefundResponse getRefund(String merchantId, String refundId);
}
