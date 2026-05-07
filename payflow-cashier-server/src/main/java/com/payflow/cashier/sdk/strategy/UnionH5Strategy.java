package com.payflow.cashier.sdk.strategy;

import com.payflow.common.exception.BizException;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.payment.core.NotifyResult;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.RefundResult;
import com.payflow.payment.union.UnionPayIntegration;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 银联云闪付 H5 占位策略：返回开放平台入口，真实网关需替换为银联全渠道/无跳转产品地址。
 */
@Slf4j
@Component("union_h5PayStrategy")
public class UnionH5Strategy implements PayStrategy {

    @Override
    public PayMethod getPayMethod() {
        return PayMethod.UNION_H5;
    }

    @Override
    public PayResult pay(String orderId, Long amount, String subject,
                         String returnUrl, String notifyUrl,
                         ChannelConfigHolder account,
                         Map<String, String> extraParams) {
        String q = "orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8)
                + "&amount=" + (amount != null ? amount : 0);
        return PayResult.builder()
                .status("PROCESSING")
                .action("REDIRECT")
                .h5Url(UnionPayIntegration.PLACEHOLDER_H5_URL + "?" + q)
                .channelTradeNo(orderId)
                .build();
    }

    @Override
    public RefundResult refund(String tradeNo, Long refundAmount,
                               String reason, ChannelConfigHolder account) {
        throw new BizException(6007, "银联 H5 占位模块未接入退款接口");
    }

    @Override
    public NotifyResult parseNotify(HttpServletRequest request) {
        log.warn("收到银联回调但未实现验签逻辑");
        return NotifyResult.builder()
                .success(false)
                .aliReply("fail")
                .errorMsg("银联回调未实现")
                .build();
    }
}
