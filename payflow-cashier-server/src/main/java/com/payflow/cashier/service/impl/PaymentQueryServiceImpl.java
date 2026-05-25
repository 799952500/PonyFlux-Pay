package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.dto.PaymentChannelQueryResult;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.service.PayChannelService;
import com.payflow.cashier.service.PayNotifyService;
import com.payflow.cashier.service.PaymentQueryService;
import com.payflow.common.exception.BizException;
import com.payflow.payment.wechat.WxPayNativeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * 支付机构查单与本地同步。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentQueryServiceImpl implements PaymentQueryService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final PayChannelService payChannelService;
    private final WxPayNativeHandler wxPayNativeHandler;
    private final PayNotifyService payNotifyService;

    @Override
    public PaymentChannelQueryResult queryChannelAndOptionalSync(String paymentId, boolean sync) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentId, paymentId));
        if (payment == null) {
            throw new BizException(6004, "支付记录不存在: " + paymentId);
        }

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, payment.getOrderId()));
        if (order == null) {
            throw new BizException(6001, "关联订单不存在: " + payment.getOrderId());
        }

        String payChannel = payment.getPayChannel();
        boolean supported = isWechatChannel(payChannel);
        Boolean channelPaid = null;
        String message;

        if (!supported) {
            message = "当前渠道暂不支持主动向支付机构查单，请联系研发扩展";
        } else {
            PayChannelAccount account = payChannelService.routeToAccount(
                    order.getMerchantId(), payChannel);
            if (account == null) {
                throw new BizException(6002, "找不到支付账户，无法查单");
            }
            channelPaid = wxPayNativeHandler.queryOutTradeNoSuccess(payment.getOrderId(), account);
            message = Boolean.TRUE.equals(channelPaid)
                    ? "支付机构侧订单状态为已支付"
                    : "支付机构侧订单未支付或查单失败";
        }

        boolean synced = false;
        if (sync && Boolean.TRUE.equals(channelPaid)) {
            if (Payment.STATUS_PROCESSING.equals(payment.getStatus())) {
                payNotifyService.handlePaymentSuccess(payment.getOrderId(), payment.getChannelTransactionId());
                synced = true;
                message = message + "；已同步本地支付与订单为成功";
                payment = paymentMapper.selectOne(
                        new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentId, paymentId));
            } else if (Payment.STATUS_SUCCESS.equals(payment.getStatus())
                    || Payment.STATUS_PARTIAL_REFUND.equals(payment.getStatus())) {
                message = message + "；本地已是成功状态，无需同步";
            } else {
                message = message + "；本地状态为 " + payment.getStatus() + "，未执行同步";
            }
        } else if (sync && !Boolean.TRUE.equals(channelPaid)) {
            message = message + "；渠道未支付，未同步本地";
        }

        return PaymentChannelQueryResult.builder()
                .paymentId(paymentId)
                .orderId(payment.getOrderId())
                .payChannel(payChannel)
                .localStatus(payment.getStatus())
                .channelPaid(channelPaid)
                .synced(synced)
                .channelQuerySupported(supported)
                .message(message)
                .build();
    }

    private static boolean isWechatChannel(String payChannel) {
        if (payChannel == null || payChannel.isBlank()) {
            return false;
        }
        String n = payChannel.trim().toUpperCase(Locale.ROOT).replace('-', '_');
        return "WECHAT_PAY".equals(n) || "WECHAT".equals(n) || "WXPAY".equals(n);
    }
}
