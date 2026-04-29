package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.dto.RefundRequest;
import com.payflow.cashier.dto.RefundResponse;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.Refund;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.mapper.RefundMapper;
import com.payflow.cashier.service.OrderMqProducer;
import com.payflow.cashier.service.PayChannelService;
import com.payflow.cashier.service.RefundService;
import com.payflow.cashier.util.SignUtils;
import com.payflow.common.exception.BizException;
import com.payflow.payment.alipay.AliPayQrHandler;
import com.payflow.payment.wechat.WxPayNativeHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private static final String CHANNEL_WECHAT = "WECHAT_PAY";
    private static final String CHANNEL_ALIPAY = "ALIPAY";

    private final RefundMapper refundMapper;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final PayChannelService payChannelService;
    private final WxPayNativeHandler wxPayNativeHandler;
    private final AliPayQrHandler aliPayQrHandler;
    private final OrderMqProducer orderMqProducer;

    @Override
    public RefundResponse getRefund(String merchantId, String refundId) {
        Refund refund = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>().eq(Refund::getRefundId, refundId));
        if (refund == null) {
            throw new BizException(6004, "退款记录不存在: " + refundId);
        }
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, refund.getOrderId()));
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw new BizException(6005, "无权查询此退款");
        }
        return RefundResponse.builder()
                .refundId(refund.getRefundId())
                .paymentId(refund.getPaymentId())
                .channelRefundNo(refund.getChannelRefundNo())
                .status(refund.getStatus())
                .refundAmount(refund.getRefundAmount())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundResponse refund(String merchantId, RefundRequest request) {
        String paymentId = request.getPaymentId();
        Long refundAmount = request.getRefundAmount();

        log.info("申请退款: merchantId={}, paymentId={}, refundAmount={}, reason={}",
                merchantId, paymentId, refundAmount, request.getReason());

        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getPaymentId, paymentId));
        if (payment == null) {
            throw new BizException(6004, "支付记录不存在: " + paymentId);
        }

        if (!Payment.STATUS_SUCCESS.equals(payment.getStatus())
                && !Payment.STATUS_PARTIAL_REFUND.equals(payment.getStatus())) {
            throw new BizException(6010,
                    "支付状态不可退款，当前状态: " + payment.getStatus());
        }

        if (refundAmount > payment.getAmount()) {
            throw new BizException(6011,
                    "退款金额超出实际支付金额: 支付=" + payment.getAmount()
                            + "，申请=" + refundAmount);
        }

        long alreadyRefunded = sumRefundedAmount(paymentId);
        if (alreadyRefunded + refundAmount > payment.getAmount()) {
            throw new BizException(6011,
                    "累计退款超出支付金额: 已退=" + alreadyRefunded + "，本次=" + refundAmount);
        }

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderId, payment.getOrderId()));
        if (order == null) {
            throw new BizException(6001, "关联订单不存在: " + payment.getOrderId());
        }
        if (!merchantId.equals(order.getMerchantId())) {
            throw new BizException(6005, "无权对此支付发起退款");
        }

        PayChannelAccount account = payChannelService.routeToAccount(
                order.getMerchantId(), payment.getPayChannel());
        if (account == null) {
            throw new BizException(6002, "找不到支付账户，无法处理退款");
        }

        String refundId = SignUtils.generatePaymentId();
        Refund refund = Refund.builder()
                .refundId(refundId)
                .paymentId(paymentId)
                .orderId(payment.getOrderId())
                .payChannel(payment.getPayChannel())
                .refundAmount(refundAmount)
                .reason(request.getReason())
                .status(Refund.STATUS_REFUNDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        refundMapper.insert(refund);

        String channelRefundNo;
        try {
            if (CHANNEL_WECHAT.equals(payment.getPayChannel())) {
                channelRefundNo = invokeWxRefund(
                        payment.getOrderId(), refundId, refundAmount,
                        payment.getAmount(), request.getReason(), account);
            } else if (CHANNEL_ALIPAY.equals(payment.getPayChannel())) {
                channelRefundNo = invokeAliRefund(
                        payment.getChannelTransactionId(), refundId,
                        refundAmount, request.getReason(), account);
            } else {
                throw new BizException(6007, "暂不支持该渠道的退款: " + payment.getPayChannel());
            }
        } catch (BizException e) {
            refund.setStatus(Refund.STATUS_FAILED);
            refund.setUpdatedAt(LocalDateTime.now());
            refundMapper.updateById(refund);
            throw e;
        }

        refund.setChannelRefundNo(channelRefundNo);
        refund.setStatus(Refund.STATUS_REFUNDED);
        refund.setUpdatedAt(LocalDateTime.now());
        refundMapper.updateById(refund);

        boolean fullRefund = alreadyRefunded + refundAmount >= payment.getAmount();
        payment.setStatus(fullRefund ? Payment.STATUS_REFUNDED : Payment.STATUS_PARTIAL_REFUND);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        log.info("退款成功: paymentId={}, refundId={}, channelRefundNo={}",
                paymentId, refundId, channelRefundNo);

        try {
            orderMqProducer.sendRefundResultNotify(
                    payment.getOrderId(),
                    fullRefund ? Payment.STATUS_REFUNDED : Payment.STATUS_PARTIAL_REFUND,
                    paymentId,
                    refundId,
                    refundAmount);
        } catch (Exception e) {
            log.warn("发送退款结果商户通知失败: orderId={}, error={}",
                    payment.getOrderId(), e.getMessage());
        }

        return RefundResponse.builder()
                .refundId(refundId)
                .paymentId(paymentId)
                .channelRefundNo(channelRefundNo)
                .status(Refund.STATUS_REFUNDED)
                .refundAmount(refundAmount)
                .build();
    }

    private long sumRefundedAmount(String paymentId) {
        List<Refund> list = refundMapper.selectList(
                new LambdaQueryWrapper<Refund>()
                        .eq(Refund::getPaymentId, paymentId)
                        .eq(Refund::getStatus, Refund.STATUS_REFUNDED));
        long sum = 0L;
        for (Refund r : list) {
            if (r.getRefundAmount() != null) {
                sum += r.getRefundAmount();
            }
        }
        return sum;
    }

    /**
     * 调用微信支付退款 API。
     */
    private String invokeWxRefund(String orderId, String refundId,
                                  Long refundAmount, Long totalAmount,
                                  String reason, PayChannelAccount account) {
        var resp = wxPayNativeHandler.refund(
                orderId, refundId, refundAmount, totalAmount, reason, account);
        return resp.getPrepayId();
    }

    /**
     * 调用支付宝退款 API。
     */
    private String invokeAliRefund(String tradeNo, String refundId,
                                   Long refundAmount, String reason,
                                   PayChannelAccount account) {
        var resp = aliPayQrHandler.refund(tradeNo, refundAmount, reason, account);
        return resp.getTradeNo();
    }
}
