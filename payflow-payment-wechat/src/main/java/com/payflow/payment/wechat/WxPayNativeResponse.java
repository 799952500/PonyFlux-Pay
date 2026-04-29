package com.payflow.payment.wechat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 微信支付 Native 扫码支付响应。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WxPayNativeResponse {

    /** 交易类型：NATIVE */
    private String tradeType;

    /** 预支付交易会话标识（prepay_id） */
    private String prepayId;

    /** 二维码链接（code_url），用于生成付款二维码 */
    private String codeUrl;

    /** 商户订单号 */
    private String outTradeNo;
}
