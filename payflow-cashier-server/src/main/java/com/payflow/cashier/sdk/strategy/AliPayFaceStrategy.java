package com.payflow.cashier.sdk.strategy;

import com.payflow.cashier.sdk.alipay.AliPayNotifyHelper;
import com.payflow.payment.alipay.AliPayBarcodeHandler;
import com.payflow.payment.alipay.AliPayQrHandler;
import com.payflow.payment.alipay.AliPayRefundResponse;
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
 * 支付宝条码/当面付（被扫）策略。
 */
@Slf4j
@Component("alipay_facePayStrategy")
@RequiredArgsConstructor
public class AliPayFaceStrategy implements PayStrategy {

    private final AliPayBarcodeHandler aliPayBarcodeHandler;
    private final AliPayQrHandler aliPayQrHandler;
    private final AliPayNotifyHelper aliPayNotifyHelper;

    @Override
    public PayMethod getPayMethod() {
        return PayMethod.ALIPAY_FACE;
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
        AliPayBarcodeHandler.AliPayBarcodePayResult r = aliPayBarcodeHandler.pay(
                orderId, amount, subject, notifyUrl, authCode, account);
        if (r.success() && r.tradeNo() != null && !r.tradeNo().isBlank()) {
            return PayResult.builder()
                    .status("SUCCESS")
                    .action("COMPLETE")
                    .paidImmediately(Boolean.TRUE)
                    .channelTransactionId(r.tradeNo())
                    .channelTradeNo(orderId)
                    .build();
        }
        return PayResult.builder()
                .status("PROCESSING")
                .action("BARCODE_POLL")
                .channelTradeNo(orderId)
                .build();
    }

    @Override
    public RefundResult refund(String tradeNo, Long refundAmount,
                               String reason, ChannelConfigHolder account) {
        AliPayRefundResponse resp = aliPayQrHandler.refund(tradeNo, refundAmount, reason, account);
        return RefundResult.builder()
                .success(true)
                .refundId(resp.getTradeNo())
                .refundStatus(resp.getGmtRefundPay())
                .build();
    }

    @Override
    public NotifyResult parseNotify(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        Map<String, String> flatParams = new HashMap<>();
        params.forEach((k, v) -> flatParams.put(k, v.length > 0 ? v[0] : null));
        return aliPayNotifyHelper.parseNotify(flatParams);
    }
}
