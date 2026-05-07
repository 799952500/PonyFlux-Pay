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
import com.payflow.cashier.openservice.payment.PayChannelPaymentOpenService;
import com.payflow.cashier.openservice.payment.PayChannelPaymentOpenServiceLocator;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.PayResult;
import com.payflow.cashier.service.OrderService;
import com.payflow.cashier.routing.ChannelHealthRedisService;
import com.payflow.cashier.service.PayNotifyService;
import com.payflow.cashier.service.PaymentService;
import com.payflow.cashier.service.PayChannelService;
import com.payflow.cashier.util.SignUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付服务实现：按渠道路由到 {channelCode}PaymentOpenService（如 alipayPaymentOpenService、wxpayPaymentOpenService）。
 *
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
    private final PayChannelPaymentOpenServiceLocator paymentOpenServiceLocator;
    private final PayflowProperties payflowProperties;
    private final PayNotifyService payNotifyService;
    private final ChannelHealthRedisService channelHealthRedisService;

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
                .accountCode(account.getAccountCode())
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
        try {
            CreatePaymentResponse response = dispatchToHandler(
                    orderId, order.getAmount(), order.getSubject(), payChannel, payMethod,
                    internalReturnUrl, notifyUrl, account, request);

            response.setPaymentId(paymentId);
            response.setOrderId(orderId);
            if (Boolean.TRUE.equals(response.getPaidImmediately())) {
                response.setStatus(Payment.STATUS_SUCCESS);
            } else {
                response.setStatus("PROCESSING");
            }

            log.info("支付下单完成: orderId={}, paymentId={}, action={}, paidImmediately={}",
                    orderId, paymentId, response.getAction(), response.getPaidImmediately());

            return response;
        } catch (BizException e) {
            channelHealthRedisService.recordOutcome(account.getAccountCode(), false);
            throw e;
        }
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
            String payChannel,
            String payMethod,
            String returnUrl, String notifyUrl,
            PayChannelAccount account,
            CreatePaymentRequest request) {

        String channelCode = toNotifyChannelCode(payChannel);
        PayChannelPaymentOpenService openService = paymentOpenServiceLocator.requireByChannelCode(channelCode);
        Map<String, String> channelExtras = buildChannelExtras(request);
        PayResult result = openService.pay(orderId, amount, subject, payMethod, returnUrl, notifyUrl, account, channelExtras);

        CreatePaymentResponse resp = convertToResponse(result);
        if (Boolean.TRUE.equals(result.getPaidImmediately())) {
            payNotifyService.handlePaymentSuccess(orderId, result.getChannelTransactionId());
            resp.setPaidImmediately(Boolean.TRUE);
            resp.setChannelTransactionId(result.getChannelTransactionId());
            resp.setStatus(Payment.STATUS_SUCCESS);
        } else if (result.getChannelTransactionId() != null) {
            resp.setChannelTransactionId(result.getChannelTransactionId());
        }
        return resp;
    }

    /**
     * 组装渠道扩展参数（openid、付款码等）。
     */
    private Map<String, String> buildChannelExtras(CreatePaymentRequest request) {
        Map<String, String> m = new HashMap<>();
        if (request.getOpenId() != null && !request.getOpenId().isBlank()) {
            m.put("openid", request.getOpenId().trim());
        }
        if (request.getAuthCode() != null && !request.getAuthCode().isBlank()) {
            m.put("auth_code", request.getAuthCode().trim());
        }
        if (request.getAlipayUserId() != null && !request.getAlipayUserId().isBlank()) {
            m.put("alipay_user_id", request.getAlipayUserId().trim());
        }
        return m;
    }

    private CreatePaymentResponse convertToResponse(PayResult r) {
        String rawAction = r.getAction();
        String action = rawAction;
        if ("INVOKE_APP".equals(rawAction) || "INVOKE_JSAPI".equals(rawAction)) {
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

        if ("INVOKE_APP".equals(rawAction) || "INVOKE_JSAPI".equals(rawAction)) {
            if (r.getInvokeParams() != null) {
                var params = r.getInvokeParams();
                builder.invokeParams(InvokeParams.builder()
                        .appId(firstNonBlank(params.get("appId"), params.get("appid")))
                        .partnerId(params.get("partnerid"))
                        .prepayId(params.get("prepayid"))
                        .package_(params.get("package"))
                        .nonceStr(firstNonBlank(params.get("nonceStr"), params.get("noncestr")))
                        .timestamp(firstNonBlank(params.get("timeStamp"), params.get("timestamp")))
                        .sign(params.get("sign"))
                        .signType(params.get("signType"))
                        .build());
            } else if (r.getAppParams() != null) {
                builder.invokeParams(InvokeParams.builder()
                        .package_(r.getAppParams())
                        .build());
            }
        }

        return builder.build();
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
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
        // 约定：回调路径强制携带“渠道编码”，避免服务端轮询解析
        String channelCode = toNotifyChannelCode(payChannel);
        return base + n.getUnifiedPath() + "/" + channelCode;
    }

    /**
     * 将订单/支付中的 payChannel 转换为回调路径使用的渠道编码。
     *
     * @param payChannel 订单/支付渠道（例如 WECHAT_PAY / ALIPAY / UNION_PAY）
     * @return 渠道编码（wxpay/alipay/unionpay）
     */
    private String toNotifyChannelCode(String payChannel) {
        if (Order.CHANNEL_WECHAT_PAY.equals(payChannel)) {
            return "wxpay";
        }
        if (Order.CHANNEL_ALIPAY.equals(payChannel)) {
            return "alipay";
        }
        if (Order.CHANNEL_UNION_PAY.equals(payChannel)) {
            return "unionpay";
        }
        throw new BizException(6007, "不支持的支付渠道: " + payChannel);
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
