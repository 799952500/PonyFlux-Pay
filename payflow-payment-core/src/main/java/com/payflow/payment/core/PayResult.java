package com.payflow.payment.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 统一支付结果。
  * @author Lucas
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

    /**
     * 渠道同步返回已成功（如付款码/条码即时成功），网关应直接落单并触发与异步回调一致的后处理。
     */
    private Boolean paidImmediately;

    /** 渠道侧交易号（同步成功时填充） */
    private String channelTransactionId;
}
