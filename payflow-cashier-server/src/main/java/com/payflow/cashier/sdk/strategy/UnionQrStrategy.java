package com.payflow.cashier.sdk.strategy;

import com.payflow.common.exception.BizException;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.payment.core.NotifyResult;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.RefundResult;
import com.payflow.payment.union.UnionPayAccountConfig;
import com.payflow.payment.union.UnionPayConfigLoader;
import com.payflow.payment.union.UnionPayQrHandler;
import com.payflow.payment.union.UnionPayRefundHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 银联扫码支付策略。
 */
@Slf4j
@Component("union_qrPayStrategy")
public class UnionQrStrategy implements PayStrategy {

    @Override
    public PayMethod getPayMethod() {
        return PayMethod.UNION_QR;
    }

    @Override
    public PayResult pay(String orderId, Long amount, String subject,
                         String returnUrl, String notifyUrl,
                         ChannelConfigHolder account,
                         Map<String, String> extraParams) {
        UnionPayAccountConfig config = UnionPayConfigLoader.load(account);
        UnionPayQrHandler handler = new UnionPayQrHandler();
        UnionPayQrHandler.QrPayResult result = handler.pay(orderId, amount, subject, notifyUrl, config);
        return PayResult.builder()
                .status("PROCESSING")
                .action("QR_CODE")
                .qrCodeUrl(result.qrCode())
                .channelTradeNo(result.queryId())
                .build();
    }

    @Override
    public RefundResult refund(String tradeNo, Long refundAmount,
                               String reason, ChannelConfigHolder account) {
        UnionPayAccountConfig config = UnionPayConfigLoader.load(account);
        UnionPayRefundHandler handler = new UnionPayRefundHandler();
        // tradeNo = orderId; for MVP, origQryId defaults to orderId
        // TODO: resolve real origQryId from payment record in production
        UnionPayRefundHandler.RefundResult result = handler.refund(
                tradeNo, tradeNo, refundAmount, reason, config);
        return RefundResult.builder()
                .success(true)
                .refundId(tradeNo)
                .channelTradeNo(result.queryId())
                .build();
    }

    @Override
    public NotifyResult parseNotify(HttpServletRequest request) {
        log.warn("UnionQrStrategy.parseNotify 不应被直接调用，通知处理由 UnionPayOpenService 统一分发");
        return NotifyResult.builder()
                .success(false)
                .errorMsg("请使用 UnionPayOpenService 处理通知")
                .build();
    }
}
