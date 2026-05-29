package com.payflow.cashier.openservice.payment.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.openservice.payment.ChannelOrderQueryResult;
import com.payflow.cashier.openservice.payment.PayChannelPaymentOpenService;
import com.payflow.cashier.sdk.PayStrategyLocator;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.PayMethod;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.RefundResult;
import com.payflow.payment.union.UnionPayConfigLoader;
import com.payflow.payment.union.UnionPayQrHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 银联/云闪付支付开放服务。
 */
@Slf4j
@Service("unionpayPaymentOpenService")
@RequiredArgsConstructor
public class UnionPayPaymentOpenService implements PayChannelPaymentOpenService {

    private final PayStrategyLocator payStrategyLocator;
    private final PaymentMapper paymentMapper;

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
        if (methodEnum != PayMethod.UNION_H5 && methodEnum != PayMethod.UNION_QR) {
            throw new BizException(7103, "支付方式与渠道不匹配: channel=UNION_PAY, payMethod=" + payMethod);
        }
        PayStrategy strategy = payStrategyLocator.requireByPayMethodCode(payMethod);
        PayResult result = strategy.pay(orderId, amount, subject, returnUrl, notifyUrl, account, channelExtras);
        log.info("银联下单完成: orderId={}, payMethod={}, action={}, channelTradeNo={}",
                orderId, payMethod, result.getAction(), result.getChannelTradeNo());
        return result;
    }

    @Override
    public RefundResult refund(String orderId, String refundId,
                               Long refundAmount, Long totalAmount,
                               String reason, PayChannelAccount account) {
        PayStrategy strategy = payStrategyLocator.requireByPayMethodCode(PayMethod.UNION_H5.getCode());
        RefundResult result = strategy.refund(orderId, refundAmount, reason, account);
        log.info("银联退款完成: orderId={}, refundId={}, success={}", orderId, refundId, result.isSuccess());
        return result;
    }

    @Override
    public ChannelOrderQueryResult queryOrder(String orderId, PayChannelAccount account) {
        var config = UnionPayConfigLoader.load(account);
        Payment payment = paymentMapper.selectOne(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getOrderId, orderId)
                .orderByDesc(Payment::getCreatedAt)
                .last("LIMIT 1"));
        LocalDateTime txnTime = payment != null && payment.getCreatedAt() != null
                ? payment.getCreatedAt()
                : LocalDateTime.now();
        boolean paid = new UnionPayQrHandler().queryOrderSuccess(orderId, txnTime, config);
        return ChannelOrderQueryResult.of(paid, null,
                paid ? "银联侧订单已支付" : "银联侧订单未支付或查单失败");
    }
}
