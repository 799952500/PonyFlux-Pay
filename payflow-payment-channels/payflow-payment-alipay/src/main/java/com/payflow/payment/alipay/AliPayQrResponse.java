package com.payflow.payment.alipay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付宝扫码支付（Native）响应。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliPayQrResponse {

    /** 商户订单号（out_trade_no） */
    private String outTradeNo;

    /** 二维码链接，用户扫码后拉起支付宝付款 */
    private String qrCode;
}
