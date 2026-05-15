package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.dto.RefundRequest;
import com.payflow.cashier.dto.RefundResponse;
import com.payflow.cashier.entity.Merchant;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.Refund;
import com.payflow.cashier.mapper.MerchantMapper;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.mapper.RefundMapper;
import com.payflow.cashier.openservice.payment.PayChannelPaymentOpenService;
import com.payflow.cashier.openservice.payment.PayChannelPaymentOpenServiceLocator;
import com.payflow.cashier.service.OrderMqProducer;
import com.payflow.cashier.service.PayChannelService;
import com.payflow.cashier.service.RefundService;
import com.payflow.cashier.util.SignUtils;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.RefundResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * 退款服务实现。
 *
 * <p>使用 {@link PayChannelPaymentOpenServiceLocator} 定位渠道退款处理器，
 * 与支付下单保持一致的路由策略，新增渠道只需添加 OpenService 实现即可。</p>
 *
 * @author Lucas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundMapper refundMapper;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final MerchantMapper merchantMapper;
    private final PayChannelService payChannelService;
    private final PayChannelPaymentOpenServiceLocator paymentOpenServiceLocator;
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

        validatePaymentRefundable(payment);
        validateRefundAmount(payment, paymentId, refundAmount);

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getOrderId, payment.getOrderId()));
        if (order == null) {
            throw new BizException(6001, "关联订单不存在: " + payment.getOrderId());
        }
        if (!merchantId.equals(order.getMerchantId())) {
            throw new BizException(6005, "无权对此支付发起退款");
        }

        // 校验商户状态（非 ACTIVE 商户禁止退款）
        Merchant merchant = merchantMapper.selectOne(
                new LambdaQueryWrapper<Merchant>()
                        .eq(Merchant::getMerchantId, merchantId)
                        .select(Merchant::getStatus));
        if (merchant == null) {
            throw new BizException(6004, "商户不存在: " + merchantId);
        }
        if (!Merchant.STATUS_ACTIVE.equals(merchant.getStatus())) {
            throw new BizException(5001, "商户已暂停服务（" + merchant.getStatus() + "）");
        }

        PayChannelAccount account = resolveChannelAccount(order, payment);
        String refundId = SignUtils.generatePaymentId();
        Refund refund = createRefundRecord(refundId, payment, refundAmount, request.getReason());

        String channelRefundNo = doChannelRefund(refund, payment, account);

        boolean fullRefund = finalizeRefund(refund, channelRefundNo, payment, refundAmount);
        notifyRefundResult(order, payment, refund, refundAmount, fullRefund);

        return RefundResponse.builder()
                .refundId(refundId)
                .paymentId(paymentId)
                .channelRefundNo(channelRefundNo)
                .status(Refund.STATUS_REFUNDED)
                .refundAmount(refundAmount)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RefundResponse executeApprovedRefund(String refundId) {
        Refund refund = refundMapper.selectOne(
                new LambdaQueryWrapper<Refund>().eq(Refund::getRefundId, refundId));
        if (refund == null) {
            throw new BizException(6004, "退款记录不存在: " + refundId);
        }
        if (!Refund.STATUS_REFUNDING.equals(refund.getStatus())) {
            throw new BizException(6012, "当前状态不允许执行渠道退款: " + refund.getStatus());
        }

        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentId, refund.getPaymentId()));
        if (payment == null) {
            throw new BizException(6004, "支付记录不存在: " + refund.getPaymentId());
        }

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, payment.getOrderId()));
        if (order == null) {
            throw new BizException(6001, "关联订单不存在: " + payment.getOrderId());
        }

        PayChannelAccount account = resolveChannelAccount(order, payment);
        String channelRefundNo = doChannelRefund(refund, payment, account);

        boolean fullRefund = finalizeRefund(refund, channelRefundNo, payment, refund.getRefundAmount());
        notifyRefundResult(order, payment, refund, refund.getRefundAmount(), fullRefund);

        return RefundResponse.builder()
                .refundId(refundId)
                .paymentId(refund.getPaymentId())
                .channelRefundNo(channelRefundNo)
                .status(Refund.STATUS_REFUNDED)
                .refundAmount(refund.getRefundAmount())
                .build();
    }

    // ==================== 私有方法 ====================

    /**
     * 校验支付状态是否可退款。
     *
     * @param payment 支付记录
     */
    private void validatePaymentRefundable(Payment payment) {
        if (!Payment.STATUS_SUCCESS.equals(payment.getStatus())
                && !Payment.STATUS_PARTIAL_REFUND.equals(payment.getStatus())) {
            throw new BizException(6010,
                    "支付状态不可退款，当前状态: " + payment.getStatus());
        }
    }

    /**
     * 校验退款金额是否合法（不超过支付金额、累计不超额）。
     *
     * @param payment      支付记录
     * @param paymentId    支付ID
     * @param refundAmount 本次退款金额
     */
    private void validateRefundAmount(Payment payment, String paymentId, Long refundAmount) {
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
    }

    /**
     * 解析渠道账户配置。
     *
     * @param order   订单
     * @param payment 支付记录
     * @return 渠道账户
     */
    private PayChannelAccount resolveChannelAccount(Order order, Payment payment) {
        PayChannelAccount account = payChannelService.routeToAccount(
                order.getMerchantId(), payment.getPayChannel());
        if (account == null) {
            throw new BizException(6002, "找不到支付账户，无法处理退款");
        }
        return account;
    }

    /**
     * 创建退款记录（初始状态：REFUNDING）。
     *
     * @param refundId     退款ID
     * @param payment      支付记录
     * @param refundAmount 退款金额
     * @param reason       退款原因
     * @return 退款实体
     */
    private Refund createRefundRecord(String refundId, Payment payment,
                                       Long refundAmount, String reason) {
        Refund refund = Refund.builder()
                .refundId(refundId)
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .payChannel(payment.getPayChannel())
                .refundAmount(refundAmount)
                .reason(reason)
                .status(Refund.STATUS_REFUNDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        refundMapper.insert(refund);
        return refund;
    }

    /**
     * 统一渠道退款入口：通过 OpenService 定位器路由到对应渠道实现。
     *
     * <p>退款失败时将退款记录标记为 FAILED，不抛出异常中断事务。</p>
     *
     * @param refund  退款记录
     * @param payment 支付记录
     * @param account 渠道账户
     * @return 渠道退款单号
     */
    private String doChannelRefund(Refund refund, Payment payment, PayChannelAccount account) {
        String normalizedChannel = normalizeChannelCode(payment.getPayChannel());
        try {
            PayChannelPaymentOpenService openService =
                    paymentOpenServiceLocator.requireByChannelCode(normalizedChannel);
            RefundResult result = openService.refund(
                    payment.getOrderId(), refund.getRefundId(),
                    refund.getRefundAmount(), payment.getAmount(),
                    refund.getReason(), account);

            if (!result.isSuccess()) {
                throw new BizException(6013, "渠道退款失败: " + result.getErrorMsg());
            }
            return result.getChannelTradeNo();
        } catch (BizException e) {
            markRefundFailed(refund);
            throw e;
        }
    }

    /**
     * 完成退款：更新退款状态、支付状态。
     *
     * @param refund          退款记录
     * @param channelRefundNo 渠道退款单号
     * @param payment         支付记录
     * @param refundAmount    退款金额
     * @return 是否全额退款
     */
    private boolean finalizeRefund(Refund refund, String channelRefundNo,
                                    Payment payment, Long refundAmount) {
        // 二次校验：当前累计退款（含本次）是否超额
        long alreadyRefunded = sumRefundedAmount(refund.getPaymentId());
        long totalAfterRefund = alreadyRefunded + refundAmount;
        if (totalAfterRefund > payment.getAmount()) {
            log.error("退款超额拒绝: paymentId={}, refundId={}, alreadyRefunded={}, currentRefund={}, payAmount={}",
                    refund.getPaymentId(), refund.getRefundId(), alreadyRefunded, refundAmount, payment.getAmount());
            refund.setStatus(Refund.STATUS_FAILED);
            refund.setUpdatedAt(LocalDateTime.now());
            refundMapper.updateById(refund);
            throw new BizException(6011,
                    "累计退款超出支付金额: 已退=" + alreadyRefunded + "，本次=" + refundAmount);
        }

        refund.setChannelRefundNo(channelRefundNo);
        refund.setStatus(Refund.STATUS_REFUNDED);
        refund.setUpdatedAt(LocalDateTime.now());
        refundMapper.updateById(refund);

        boolean fullRefund = totalAfterRefund >= payment.getAmount();
        payment.setStatus(fullRefund ? Payment.STATUS_REFUNDED : Payment.STATUS_PARTIAL_REFUND);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        log.info("退款完成: paymentId={}, refundId={}, channelRefundNo={}, fullRefund={}",
                refund.getPaymentId(), refund.getRefundId(), channelRefundNo, fullRefund);
        return fullRefund;
    }

    /**
     * 发送退款结果商户通知（MQ 异步，失败不影响主流程）。
     *
     * @param order       订单
     * @param payment     支付记录
     * @param refund      退款记录
     * @param refundAmount 退款金额
     * @param fullRefund  是否全额退款
     */
    private void notifyRefundResult(Order order, Payment payment, Refund refund,
                                     Long refundAmount, boolean fullRefund) {
        try {
            orderMqProducer.sendRefundResultNotify(
                    payment.getOrderId(),
                    fullRefund ? Payment.STATUS_REFUNDED : Payment.STATUS_PARTIAL_REFUND,
                    payment.getPaymentId(),
                    refund.getRefundId(),
                    refundAmount);
        } catch (Exception e) {
            log.warn("发送退款结果商户通知失败: orderId={}, error={}",
                    payment.getOrderId(), e.getMessage());
        }
    }

    /**
     * 标记退款记录为失败状态。
     *
     * @param refund 退款记录
     */
    private void markRefundFailed(Refund refund) {
        refund.setStatus(Refund.STATUS_FAILED);
        refund.setUpdatedAt(LocalDateTime.now());
        refundMapper.updateById(refund);
    }

    /**
     * 计算指定支付已成功退款的总金额。
     *
     * @param paymentId 支付ID
     * @return 已退款总额（分）
     */
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
     * 将订单中存储的渠道编码归一化为 OpenService 定位器所需的小写格式。
     *
     * <p>兼容多种编码风格：WECHAT_PAY → wxpay、wechat_pay → wxpay、alipay → alipay。</p>
     *
     * @param payChannel 原始渠道编码
     * @return 归一化后的小写渠道编码
     */
    private static String normalizeChannelCode(String payChannel) {
        if (payChannel == null || payChannel.isBlank()) {
            throw new BizException(6007, "渠道编码为空");
        }
        String n = payChannel.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        if ("wechat_pay".equals(n) || "wxpay".equals(n) || "wechat".equals(n)) {
            return "wxpay";
        }
        if ("alipay".equals(n) || "zfb".equals(n)) {
            return "alipay";
        }
        if ("union_pay".equals(n) || "unionpay".equals(n) || "union".equals(n)) {
            return "unionpay";
        }
        throw new BizException(6007, "暂不支持该渠道的退款: " + payChannel);
    }
}
