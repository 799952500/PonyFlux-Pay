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

    /**
     * 运营审批通过后执行渠道退款（内部调用，须已由网关校验内部令牌）。
     * 仅处理 {@code REFUNDING} 状态的记录。
     */
    RefundResponse executeApprovedRefund(String refundId);
}
