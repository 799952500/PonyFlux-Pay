package com.payflow.cashier.sdk.strategy;

import com.payflow.cashier.sdk.alipay.AliPayNotifyHelper;
import com.payflow.payment.alipay.AliPayQrHandler;
import com.payflow.payment.alipay.AliPayWapHandler;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.payment.core.NotifyResult;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.RefundResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 支付宝手机网站支付（Wap）策略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliPayWapStrategy implements PayStrategy {

    private final AliPayQrHandler aliPayQrHandler;
    private final AliPayWapHandler aliPayWapHandler;
    private final AliPayNotifyHelper aliPayNotifyHelper;

    @Override
    public PayMethod getPayMethod() {
        return PayMethod.ALIPAY_WAP;
    }

    @Override
    public PayResult pay(String orderId, Long amount, String subject,
                         String returnUrl, String notifyUrl,
                         ChannelConfigHolder account,
                         Map<String, String> extraParams) {
        var resp = aliPayWapHandler.createWapOrder(
                orderId, amount, subject, returnUrl, notifyUrl, account);

        return PayResult.builder()
                .status("PROCESSING")
                .action("FORM")
                .appParams(resp.getFormHtml())
                .build();
    }

    @Override
    public RefundResult refund(String tradeNo, Long refundAmount,
                               String reason, ChannelConfigHolder account) {
        var resp = aliPayQrHandler.refund(tradeNo, refundAmount, reason, account);
        return RefundResult.builder()
                .success(true)
                .refundId(resp.getTradeNo())
                .refundStatus(resp.getGmtRefundPay())
                .channelTradeNo(tradeNo)
                .build();
    }

    @Override
    public NotifyResult parseNotify(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> flatParams = new HashMap<>();
        params.forEach((k, v) -> flatParams.put(k, v.length > 0 ? v[0] : null));
        log.info("收到支付宝WAP回调: flatParams={}", flatParams);
        return aliPayNotifyHelper.parseNotify(flatParams);
    }
}
