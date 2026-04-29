package com.payflow.payment.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一退款结果。
  * @author Lucas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResult {

    private boolean success;
    private String refundId;
    private String refundStatus;
    private String channelTradeNo;
    private String errorMsg;
}
