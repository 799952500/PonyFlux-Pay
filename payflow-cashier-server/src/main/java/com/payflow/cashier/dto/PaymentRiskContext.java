package com.payflow.cashier.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付风控上下文。
 */
@Data
@Builder
public class PaymentRiskContext {

    private String merchantId;

    private String merchantOrderNo;

    private Long amountFen;

    private String currency;

    private String channel;

    private String clientIp;

    private String mobileHash;

    private String deviceFingerprint;

    private LocalDateTime requestTime;

    private String traceId;
}
