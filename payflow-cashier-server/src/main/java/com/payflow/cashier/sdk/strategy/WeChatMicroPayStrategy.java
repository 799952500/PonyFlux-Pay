package com.payflow.cashier.sdk.strategy;

import com.payflow.cashier.sdk.wxpay.WxPayNotifyHelper;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.payment.core.NotifyResult;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.RefundResult;
import com.payflow.payment.wechat.WxPayMicropayHandler;
import com.payflow.payment.wechat.WxPayMicropayHandler.WxMicropayResult;
import com.payflow.payment.wechat.WxPayNativeHandler;
import com.payflow.payment.wechat.WxPayNativeResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 微信付款码（被扫）支付策略。
 */
@Slf4j
@Component("wechat_micropayPayStrategy")
@RequiredArgsConstructor
public class WeChatMicroPayStrategy implements PayStrategy {

    private final WxPayMicropayHandler wxPayMicropayHandler;
    private final WxPayNativeHandler wxPayNativeHandler;
    private final WxPayNotifyHelper wxPayNotifyHelper;

    @Override
    public PayMethod getPayMethod() {
        return PayMethod.WECHAT_MICROPAY;
    }

    @Override
    public PayResult pay(String orderId, Long amount, String subject,
                         String returnUrl, String notifyUrl,
                         ChannelConfigHolder account,
                         Map<String, String> extraParams) {
        String authCode = extraParams != null ? extraParams.get("auth_code") : null;
        if (authCode == null || authCode.isBlank()) {
            authCode = extraParams != null ? extraParams.get("authCode") : null;
        }
        WxMicropayResult r = wxPayMicropayHandler.codePay(
                orderId, amount, subject, notifyUrl, account, authCode);
        if (r.immediateSuccess()) {
            return PayResult.builder()
                    .status("SUCCESS")
                    .action("COMPLETE")
                    .paidImmediately(Boolean.TRUE)
                    .channelTransactionId(r.transactionId())
                    .channelTradeNo(orderId)
                    .build();
        }
        if ("USERPAYING".equals(r.tradeState())) {
            return PayResult.builder()
                    .status("PROCESSING")
                    .action("MICROPAY_POLL")
                    .channelTradeNo(orderId)
                    .build();
        }
        throw new BizException(6005, "微信付款码支付失败，trade_state=" + r.tradeState());
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
