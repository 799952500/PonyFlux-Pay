package com.payflow.recon.mapper.cashier;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付明细行（比对用）。
 *
 * @author PayFlow Team
 */
@Data
public class CashierReconPaymentRow {
    private String paymentId;
    private String orderId;
    private String payChannel;
    private String channelTransactionId;
    private Long amount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String merchantId;
}
