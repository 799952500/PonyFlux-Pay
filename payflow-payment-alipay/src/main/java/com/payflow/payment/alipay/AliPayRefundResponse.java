package com.payflow.payment.alipay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付宝退款响应。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliPayRefundResponse {

    /** 商户订单号 */
    private String outTradeNo;

    /** 支付宝交易号 */
    private String tradeNo;

    /** 买家支付宝账号（脱敏） */
    private String buyerLogonId;

    /** 退款金额（元） */
    private String refundFee;

    /** 退款时间 */
    private String gmtRefundPay;
}
