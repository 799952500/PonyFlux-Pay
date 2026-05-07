package com.payflow.cashier.sdk.strategy;

import com.payflow.cashier.sdk.wxpay.WxPayNotifyHelper;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.payment.core.NotifyResult;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.RefundResult;
import com.payflow.payment.wechat.WxPayAccountConfig;
import com.payflow.payment.wechat.WxPayConfigLoader;
import com.payflow.payment.wechat.WxPayJsapiHandler;
import com.payflow.payment.wechat.WxPayNativeHandler;
import com.payflow.payment.wechat.WxPayNativeResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信 JSAPI（公众号内 H5）支付策略。
 */
@Slf4j
@Component("wechat_jsapiPayStrategy")
@RequiredArgsConstructor
public class WeChatJsapiStrategy implements PayStrategy {

    private final WxPayJsapiHandler wxPayJsapiHandler;
    private final WxPayNativeHandler wxPayNativeHandler;
    private final WxPayNotifyHelper wxPayNotifyHelper;

    @Override
    public PayMethod getPayMethod() {
        return PayMethod.WECHAT_JSAPI;
    }

    @Override
    public PayResult pay(String orderId, Long amount, String subject,
                         String returnUrl, String notifyUrl,
                         ChannelConfigHolder account,
                         Map<String, String> extraParams) {
        String openId = extraParams != null ? extraParams.get("openid") : null;
        if (openId == null || openId.isBlank()) {
            openId = extraParams != null ? extraParams.get("openId") : null;
        }
        String prepayId = wxPayJsapiHandler.createJsapiPrepayId(
                orderId, amount, subject, notifyUrl, account, openId);
        WxPayAccountConfig config = WxPayConfigLoader.load(account);
        Map<String, String> invoke = wxPayJsapiHandler.buildJsapiInvokeParams(config, prepayId);
        String pkg = "prepay_id=" + prepayId;
        Map<String, String> lower = new HashMap<>();
        lower.put("appid", config.appId);
        lower.put("appId", config.appId);
        lower.put("partnerid", config.mchId);
        lower.put("prepayid", prepayId);
        lower.put("package", pkg);
        lower.put("noncestr", invoke.get("nonceStr"));
        lower.put("timestamp", invoke.get("timeStamp"));
        lower.put("sign", invoke.get("paySign"));
        lower.put("signType", invoke.get("signType"));

        return PayResult.builder()
                .status("PROCESSING")
                .action("INVOKE_JSAPI")
                .invokeParams(lower)
                .channelTradeNo(prepayId)
                .build();
    }

    @Override
    public RefundResult refund(String tradeNo, Long refundAmount,
                               String reason, ChannelConfigHolder account) {
        WxPayNativeResponse resp = wxPayNativeHandler.refund(
                tradeNo, tradeNo, refundAmount, refundAmount, reason, account);
        return RefundResult.builder()
                .success(true)
                .refundId(resp.getPrepayId())
                .refundStatus("REFUNDED")
                .channelTradeNo(resp.getOutTradeNo())
                .build();
    }

    @Override
    public NotifyResult parseNotify(HttpServletRequest request) {
        return readWxBody(request);
    }

    private NotifyResult readWxBody(HttpServletRequest request) {
        String serial = request.getHeader("Wechatpay-Serial");
        String signature = request.getHeader("Wechatpay-Signature");
        String timestamp = request.getHeader("Wechatpay-Timestamp");
        String nonce = request.getHeader("Wechatpay-Nonce");
        String body;
        try {
            body = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("读取微信回调 body 失败", e);
            return NotifyResult.builder()
                    .success(false)
                    .errorMsg(e.getMessage())
                    .wxReply("FAIL")
                    .build();
        }
        return wxPayNotifyHelper.parseNotify(serial, signature, timestamp, nonce, body);
    }
}
