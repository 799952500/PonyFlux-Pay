package com.payflow.cashier.openservice.payment.impl;

import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.openservice.payment.PayChannelPaymentOpenService;
import com.payflow.cashier.sdk.PayStrategyLocator;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.RefundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 银联/云闪付支付下单开放服务（当前仅 UNION_H5 占位）。
 */
@Slf4j
@Service("unionpayPaymentOpenService")
@RequiredArgsConstructor
public class UnionPayPaymentOpenService implements PayChannelPaymentOpenService {

    private final PayStrategyLocator payStrategyLocator;

    @Override
    public String channelCode() {
        return "unionpay";
    }

    @Override
    public PayResult pay(String orderId,
                         Long amount,
                         String subject,
                         String payMethod,
                         String returnUrl,
                         String notifyUrl,
                         PayChannelAccount account,
                         Map<String, String> channelExtras) {
        PayMethod methodEnum = PayMethod.fromCode(payMethod);
        if (methodEnum == null) {
            throw new BizException(6007, "不支持的支付方式: " + payMethod);
        }
        if (methodEnum != PayMethod.UNION_H5) {
            throw new BizException(7103, "支付方式与渠道不匹配: channel=UNION_PAY, payMethod=" + payMethod);
        }
        PayStrategy strategy = payStrategyLocator.requireByPayMethodCode(payMethod);
        PayResult result = strategy.pay(orderId, amount, subject, returnUrl, notifyUrl, account, channelExtras);
        log.info("银联下单完成: orderId={}, action={}", orderId, result.getAction());
        return result;
    }

    @Override
    public RefundResult refund(String orderId, String refundId,
                               Long refundAmount, Long totalAmount,
                               String reason, PayChannelAccount account) {
        throw new BizException(6007, "银联渠道暂未接入退款");
    }
}
