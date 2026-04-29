package com.payflow.cashier.sdk.strategy;

import com.payflow.cashier.sdk.wxpay.WxPayNotifyHelper;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.payment.core.NotifyResult;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.RefundResult;
import com.payflow.payment.wechat.WxPayH5Handler;
import com.payflow.payment.wechat.WxPayNativeHandler;
import com.payflow.payment.wechat.WxPayNativeResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 微信 H5 支付策略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatH5Strategy implements PayStrategy {

    private final WxPayNativeHandler wxPayNativeHandler;
    private final WxPayH5Handler wxPayH5Handler;
    private final WxPayNotifyHelper wxPayNotifyHelper;

    @Override
    public PayMethod getPayMethod() {
        return PayMethod.WECHAT_H5;
    }

    @Override
    public PayResult pay(String orderId, Long amount, String subject,
                         String returnUrl, String notifyUrl,
                         ChannelConfigHolder account,
                         Map<String, String> extraParams) {
        String h5Url = wxPayH5Handler.createH5Order(
                orderId, amount, subject, notifyUrl, returnUrl, account);

        return PayResult.builder()
                .status("PROCESSING")
                .action("REDIRECT")
                .h5Url(h5Url)
                .build();
    }

    @Override
    public RefundResult refund(String tradeNo, Long refundAmount,
                               String reason, ChannelConfigHolder account) {
        WxPayNativeResponse resp = wxPayNativeHandler.refund(
                tradeNo, tradeNo,
                refundAmount, refundAmount, reason, account);
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
        log.info("收到微信H5支付回调: serial={}, timestamp={}", serial, timestamp);
        return wxPayNotifyHelper.parseNotify(serial, signature, timestamp, nonce, body);
    }
}
