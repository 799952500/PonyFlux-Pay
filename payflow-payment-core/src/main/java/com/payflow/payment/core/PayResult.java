package com.payflow.payment.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 统一支付结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayResult {

    private String status;
    private String action;
    private String qrCodeUrl;
    private String h5Url;
    private String appParams;
    private Map<String, String> invokeParams;
    private String errorMsg;
    private String channelTradeNo;
}
