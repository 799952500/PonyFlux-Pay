package com.payflow.payment.alipay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付宝订单响应（WAP/App 支付）。
 * <p>
     * 包含支付宝交易号、外部订单号、订单金额（元）和表单 HTML（WAP 场景）。
 * </p>
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliPayOrderResponse {

    /** 支付宝交易号 */
    private String tradeNo;

    /** 商户订单号（out_trade_no） */
    private String outTradeNo;

    /** 订单金额（元），字符串格式如 "0.01" */
    private String totalAmount;

    /** WAP 支付表单 HTML（仅 WAP 场景有值，App 场景为 null） */
    private String formHtml;
}
