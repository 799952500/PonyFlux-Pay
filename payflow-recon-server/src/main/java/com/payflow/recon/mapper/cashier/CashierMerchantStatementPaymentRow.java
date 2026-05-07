package com.payflow.recon.mapper.cashier;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商户对账单明细行（收银库查询）。
 *
 * @author PayFlow Team
 */
@Data
public class CashierMerchantStatementPaymentRow {
    private String paymentId;
    private String orderId;
    private String payChannel;
    private String channelTransactionId;
    private Long amount;
    private String status;
    private LocalDateTime paidAt;
}
