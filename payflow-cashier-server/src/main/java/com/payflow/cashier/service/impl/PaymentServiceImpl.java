package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.dto.CreatePaymentRequest;
import com.payflow.cashier.dto.CreatePaymentResponse;
import com.payflow.cashier.dto.InvokeParams;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.exception.BizException;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.service.OrderService;
import com.payflow.cashier.service.PaymentService;
import com.payflow.cashier.util.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付服务实现
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;

    public PaymentServiceImpl(PaymentMapper paymentMapper, OrderMapper orderMapper,
                              OrderService orderService) {
        this.paymentMapper = paymentMapper;
        this.orderMapper = orderMapper;
        this.orderService = orderService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatePaymentResponse createPayment(CreatePaymentRequest request) {
        String orderId = request.getOrderId();
        log.info("发起支付: orderId={}, payChannel={}, payMethod={}",
                orderId, request.getPayChannel(), request.getPayMethod());

        // 1. 查询订单
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
        if (order == null) {
            throw new BizException(6001, "订单不存在: " + orderId);
        }

        // 2. 校验订单状态
        if (!Order.STATUS_CREATED.equals(order.getStatus())) {
            throw new BizException(6003, "订单状态异常: " + order.getStatus() + "，无法发起支付");
        }

        // 3. 生成支付记录
        String paymentId = SignUtils.generatePaymentId();
        LocalDateTime now = LocalDateTime.now();

        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .orderId(orderId)
                .payChannel(request.getPayChannel())
                .payMethod(request.getPayMethod())
                .amount(order.getAmount())
                .status(Payment.STATUS_PROCESSING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        paymentMapper.insert(payment);

        // 4. 更新订单状态为 PAYING
        orderService.updateOrderStatus(orderId, Order.STATUS_PAYING, null);

        // 5. 根据支付方式生成响应（模拟）
        return buildPaymentResponse(request, paymentId, orderId);
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

    private CreatePaymentResponse buildPaymentResponse(CreatePaymentRequest request,
                                                         String paymentId, String orderId) {
        String payMethod = request.getPayMethod();

        // 模拟生成调起参数或二维码
        if ("WECHAT_APP".equals(payMethod) || "WECHAT_NATIVE".equals(payMethod)) {
            return CreatePaymentResponse.builder()
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .status("PROCESSING")
                    .action(CreatePaymentResponse.ACTION_QR_CODE)
                    .qrCodeUrl("weixin://wxpay/bizpayurl?pr=模拟微信二维码URL")
                    .qrCodeImage("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==")
                    .build();
        } else if ("ALIPAY_APP".equals(payMethod) || "ALIPAY_WAP".equals(payMethod)) {
            return CreatePaymentResponse.builder()
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .status("PROCESSING")
                    .action(CreatePaymentResponse.ACTION_REDIRECT)
                    .invokeParams(InvokeParams.builder()
                            .appId("2021000000000000")
                            .partnerId("2088000000000000")
                            .prepayId("prepay_id=" + UUID.randomUUID().toString().replace("-", ""))
                            .nonceStr(UUID.randomUUID().toString())
                            .timestamp(String.valueOf(System.currentTimeMillis() / 1000))
                            .package_("sign=模拟支付宝签名")
                            .sign("模拟签名")
                            .build())
                    .build();
        } else {
            return CreatePaymentResponse.builder()
                    .paymentId(paymentId)
                    .orderId(orderId)
                    .status("PROCESSING")
                    .action(CreatePaymentResponse.ACTION_QR_CODE)
                    .qrCodeUrl("https://example.com/mock-qr?orderId=" + orderId)
                    .build();
        }
    }
}
