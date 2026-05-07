package com.payflow.cashier.sdk.strategy;

import com.payflow.cashier.sdk.wxpay.WxPayNotifyHelper;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.wechat.WxPayJsapiHandler;
import com.payflow.payment.wechat.WxPayNativeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信小程序支付策略（与 JSAPI 共用统一下单接口）。
 */
@Slf4j
@Component("wechat_miniPayStrategy")
public class WeChatMiniStrategy extends WeChatJsapiStrategy {

    public WeChatMiniStrategy(WxPayJsapiHandler wxPayJsapiHandler,
                              WxPayNativeHandler wxPayNativeHandler,
                              WxPayNotifyHelper wxPayNotifyHelper) {
        super(wxPayJsapiHandler, wxPayNativeHandler, wxPayNotifyHelper);
    }

    @Override
    public PayMethod getPayMethod() {
        return PayMethod.WECHAT_MINI;
    }
}
