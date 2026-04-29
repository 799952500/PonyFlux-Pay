package com.payflow.payment.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一异步通知解析结果。
  * @author Lucas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotifyResult {

    private boolean success;
    private String tradeNo;
    private String outTradeNo;
    private Long amount;
    private String status;
    private String rawData;
    private String errorMsg;
    private String wxReply;
    private String aliReply;
}
