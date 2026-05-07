package com.payflow.admin.mapper.cashier;

import lombok.Data;

/**
 * 对账报表用：支付 + 订单商户。
 *
 * @author PayFlow Team
 */
@Data
public class ReconCashierPaymentRow {
    private String paymentId;
    private String orderId;
    private String merchantId;
    private String payChannel;
    private String accountCode;
    private String channelTransactionId;
    private Long amount;
}
