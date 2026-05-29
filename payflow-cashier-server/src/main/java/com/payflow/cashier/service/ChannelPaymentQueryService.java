package com.payflow.cashier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.openservice.payment.ChannelOrderQueryResult;
import com.payflow.cashier.openservice.payment.PayChannelPaymentOpenService;
import com.payflow.cashier.openservice.payment.PayChannelPaymentOpenServiceLocator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 统一渠道查单：超时关单、MQ 兜底、管理端查单共用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelPaymentQueryService {

    private final PaymentMapper paymentMapper;
    private final PayChannelService payChannelService;
    private final PayChannelPaymentOpenServiceLocator paymentOpenServiceLocator;

    /**
     * 若本地已有 SUCCESS 支付则视为已付；否则对 PROCESSING 支付向渠道查单。
     */
    public boolean isPaidAtChannel(Order order) {
        ChannelOrderQueryResult result = queryChannelIfNeeded(order);
        return result != null && result.isPaid();
    }

    public ChannelOrderQueryResult queryChannelIfNeeded(Order order) {
        if (order == null) {
            return ChannelOrderQueryResult.unsupported("订单不存在");
        }
        String orderId = order.getOrderId();

        Payment paidLocal = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getStatus, Payment.STATUS_SUCCESS)
                        .last("LIMIT 1"));
        if (paidLocal != null) {
            return ChannelOrderQueryResult.of(true, paidLocal.getChannelTransactionId(), "本地已成功");
        }

        Payment processing = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getStatus, Payment.STATUS_PROCESSING)
                        .orderByDesc(Payment::getCreatedAt)
                        .last("LIMIT 1"));
        if (processing == null) {
            return ChannelOrderQueryResult.unsupported("无处理中支付记录");
        }

        PayChannelAccount account = payChannelService.routeToAccount(
                order.getMerchantId(), processing.getPayChannel());
        if (account == null) {
            log.warn("查单失败：无可用账户 orderId={}", orderId);
            return ChannelOrderQueryResult.unsupported("无可用支付账户");
        }

        String channelCode = toOpenServiceChannelCode(processing.getPayChannel());
        PayChannelPaymentOpenService openService = paymentOpenServiceLocator.requireByChannelCode(channelCode);
        return openService.queryOrder(orderId, account);
    }

    private static String toOpenServiceChannelCode(String payChannel) {
        if (payChannel == null) {
            return "";
        }
        return switch (payChannel) {
            case Order.CHANNEL_WECHAT_PAY -> "wxpay";
            case Order.CHANNEL_ALIPAY -> "alipay";
            case Order.CHANNEL_UNION_PAY -> "unionpay";
            default -> payChannel.toLowerCase();
        };
    }
}
