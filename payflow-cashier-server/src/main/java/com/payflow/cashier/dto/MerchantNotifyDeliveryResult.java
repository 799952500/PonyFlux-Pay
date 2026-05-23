package com.payflow.cashier.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 单次商户 HTTP 回调执行结果。
 */
@Data
@Builder
public class MerchantNotifyDeliveryResult {

    private boolean success;
    private Integer httpStatus;
    private String responseBody;
    private String failReasonType;
    private String failReasonDetail;
    private long durationMs;
}
