package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.config.PayflowProperties;
import com.payflow.cashier.dto.CreatePaymentRequest;
import com.payflow.cashier.dto.CreatePaymentResponse;
import com.payflow.cashier.dto.InvokeParams;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.PayResult;
import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.PayStrategyRegistry;
import com.payflow.cashier.service.OrderService;
import com.payflow.cashier.service.PaymentService;
import com.payflow.cashier.service.PayChannelService;
import com.payflow.cashier.util.SignUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 支付服务实现：通过 {@link PayStrategyRegistry} 路由渠道策略。
  * @author Lucas
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;
    private final PayChannelService payChannelService;
    private final PayStrategyRegistry payStrategyRegistry;
    private final PayflowProperties payflowProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatePaymentResponse createPayment(String merchantId, CreatePaymentRequest request) {
        String orderId = request.getOrderId();
        String payChannel = request.getPayChannel();
        String payMethod = request.getPayMethod();

        log.info("发起支付: merchantId={}, orderId={}, payChannel={}, payMethod={}",
                merchantId, orderId, payChannel, payMethod);

        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
        if (order == null) {
            throw new BizException(6001, "订单不存在: " + orderId);
        }

        if (!merchantId.equals(order.getMerchantId())) {
            log.error("商户身份校验失败: 签名商户ID={}, 订单所属商户ID={}, orderId={}",
                    merchantId, order.getMerchantId(), orderId);
            throw new BizException(6005, "无权操作此订单");
        }

        if (!Order.STATUS_CREATED.equals(order.getStatus())) {
            throw new BizException(6003, "订单状态异常: " + order.getStatus() + "，无法发起支付");
        }

        PayChannelAccount account = payChannelService.routeToAccount(order.getMerchantId(), payChannel);
        if (account == null) {
            log.error("无可用支付账户: merchantId={}, payChannel={}", order.getMerchantId(), payChannel);
            throw new BizException(6002, "无可用支付账户，请联系商户配置");
        }

        String paymentId = SignUtils.generatePaymentId();
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .payChannel(payChannel)
                .payMethod(payMethod)
                .amount(order.getAmount())
                .status(Payment.STATUS_PROCESSING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        paymentMapper.insert(payment);

        orderService.updateOrderStatus(orderId, Order.STATUS_PAYING, null);

        String notifyUrl = buildNotifyUrl(payChannel);
        String internalReturnUrl = buildInternalReturnUrl(orderId);
        CreatePaymentResponse response = dispatchToHandler(
                orderId, order.getAmount(), order.getSubject(), payMethod,
                internalReturnUrl, notifyUrl, account);

        response.setPaymentId(paymentId);
        response.setOrderId(orderId);
        response.setStatus("PROCESSING");

        log.info("支付下单完成: orderId={}, paymentId={}, action={}",
                orderId, paymentId, response.getAction());

        return response;
    }

    @Override
    public String getPaymentStatus(String paymentId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentId, paymentId));
        if (payment == null) {
            throw new BizException(6004, "支付记录不存在: " + paymentId);
        }
        return payment.getStatus();
    }

    @Override
    public String getPaymentStatusForMerchant(String merchantId, String paymentId) {
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>().eq(Payment::getPaymentId, paymentId));
        if (payment == null) {
            throw new BizException(6004, "支付记录不存在: " + paymentId);
        }
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, payment.getOrderId()));
        if (order == null || !merchantId.equals(order.getMerchantId())) {
            throw new BizException(6005, "无权查询此支付记录");
        }
        return payment.getStatus();
    }

    private CreatePaymentResponse dispatchToHandler(
            String orderId, Long amount, String subject,
            String payMethod,
            String returnUrl, String notifyUrl,
            PayChannelAccount account) {

        PayStrategy strategy = payStrategyRegistry.requireByCode(payMethod);

        PayResult result = strategy.pay(orderId, amount, subject,
                returnUrl, notifyUrl, account, Map.of());

        return convertToResponse(result);
    }

    private CreatePaymentResponse convertToResponse(PayResult r) {
        String rawAction = r.getAction();
        String action = rawAction;
        if ("INVOKE_APP".equals(rawAction)) {
            action = CreatePaymentResponse.ACTION_INVOKE;
        }

        CreatePaymentResponse.CreatePaymentResponseBuilder builder = CreatePaymentResponse.builder()
                .status(r.getStatus())
                .action(action);

        if ("QR_CODE".equals(rawAction)) {
            builder.qrCodeUrl(r.getQrCodeUrl());
        }

        if ("REDIRECT".equals(rawAction)) {
            builder.redirectUrl(r.getH5Url());
        }

        if ("FORM".equals(rawAction)) {
            builder.formHtml(r.getAppParams());
        }

        if ("INVOKE_APP".equals(rawAction)) {
            if (r.getInvokeParams() != null) {
                var params = r.getInvokeParams();
                builder.invokeParams(InvokeParams.builder()
                        .appId(params.get("appid"))
                        .partnerId(params.get("partnerid"))
                        .prepayId(params.get("prepayid"))
                        .package_(params.get("package"))
                        .nonceStr(params.get("noncestr"))
                        .timestamp(params.get("timestamp"))
                        .sign(params.get("sign"))
                        .build());
            } else if (r.getAppParams() != null) {
                builder.invokeParams(InvokeParams.builder()
                        .package_(r.getAppParams())
                        .build());
            }
        }

        return builder.build();
    }

    /**
     * 按渠道生成 notify_url（须与微信/支付宝商户平台配置一致）。
     */
    private String buildNotifyUrl(String payChannel) {
        PayflowProperties.PaymentNotify n = payflowProperties.getPaymentNotify();
        String base = n.getBaseUrl();
        if (base == null || base.isBlank()) {
            throw new BizException(6002, "未配置 payflow.payment-notify.base-url");
        }
        base = base.replaceAll("/+$", "");
        return base + n.getUnifiedPath();
    }

    /**
     * 支付完成后，渠道重定向/回跳统一落到收银台内部页面。
     * <p>
     * 该 URL 本质是消费者页面地址（`/cashier/{orderId}`），不依赖 JWT。
     * </p>
     */
    private String buildInternalReturnUrl(String orderId) {
        String baseUrl = payflowProperties.getCashier().getBaseUrl();
        String sig = SignUtils.sign(payflowProperties.getSignature().getSecret(), orderId);
        return baseUrl + "/cashier/" + orderId + "?sig=" + sig;
    }
}
