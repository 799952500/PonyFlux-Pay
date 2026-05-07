package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payflow.cashier.client.AdminPaymentConfigClient;
import com.payflow.cashier.config.PayflowProperties;
import com.payflow.cashier.dto.*;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.common.exception.BizException;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.service.OrderCacheService;
import com.payflow.cashier.service.OrderMqProducer;
import com.payflow.cashier.service.OrderService;
import com.payflow.cashier.service.RiskCheckService;
import com.payflow.cashier.util.SignUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 订单服务实现
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final PayflowProperties properties;
    private final OrderCacheService orderCacheService;
    private final OrderMqProducer orderMqProducer;
    private final RiskCheckService riskCheckService;
    private final AdminPaymentConfigClient adminPaymentConfigClient;

    public OrderServiceImpl(OrderMapper orderMapper,
                            PaymentMapper paymentMapper,
                            PayflowProperties properties,
                            OrderCacheService orderCacheService,
                            OrderMqProducer orderMqProducer,
                            RiskCheckService riskCheckService,
                            AdminPaymentConfigClient adminPaymentConfigClient) {
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.properties = properties;
        this.orderCacheService = orderCacheService;
        this.orderMqProducer = orderMqProducer;
        this.riskCheckService = riskCheckService;
        this.adminPaymentConfigClient = adminPaymentConfigClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        log.info("创建订单: merchantId={}, merchantOrderNo={}, amount={}",
                request.getMerchantId(), request.getMerchantOrderNo(), request.getAmount());

        // 0. 风控校验（不通过直接抛出异常）
        riskCheckService.checkCreateOrder(request);

        // 1. 检查商户订单号重复
        Long exist = orderMapper.selectCount(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getMerchantId, request.getMerchantId())
                        .eq(Order::getMerchantOrderNo, request.getMerchantOrderNo())
        );
        if (exist > 0) {
            throw new BizException(6006, "商户订单号重复: " + request.getMerchantOrderNo());
        }

        // 2. 生成订单号
        String orderId = SignUtils.generateOrderId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusMinutes(
                request.getExpireMinutes() != null ? request.getExpireMinutes() : 30);

        // 3. 构建订单
        Order order = Order.builder()
                .orderId(orderId)
                .merchantId(request.getMerchantId())
                .merchantOrderNo(request.getMerchantOrderNo())
                .amount(request.getAmount())
                .currency(request.getCurrency() != null ? request.getCurrency() : "CNY")
                .payAmount(0L)
                .subject(request.getSubject())
                .body(request.getBody())
                .attach(request.getAttach())
                .channel(request.getChannel())
                .status(Order.STATUS_CREATED)
                .notifyUrl(request.getNotifyUrl())
                .merchantNotifyUrl(request.getNotifyUrl()) // 支付成功回调地址
                .returnUrl(request.getReturnUrl())
                .successUrl(request.getSuccessUrl())
                .failUrl(request.getFailUrl())
                .expireTime(expireTime)
                .createdAt(now)
                .updatedAt(now)
                .notifyStatus(Order.NOTIFY_STATUS_PENDING)
                .notifyRetryCount(0)
                .build();

        orderMapper.insert(order);
        log.info("订单创建成功: orderId={}", orderId);

        // 4. 缓存到 Redis（TTL=30分钟）
        try {
            orderCacheService.cacheOrder(orderId, order);
        } catch (Exception e) {
            log.warn("Redis缓存订单失败（不影响主业务）: orderId={}, error={}", orderId, e.getMessage());
        }

        // 5. 发送 MQ 延迟消息（订单超时检查）
        try {
            orderMqProducer.sendOrderTimeoutCheck(orderId, expireTime);
        } catch (Exception e) {
            log.warn("MQ发送超时检查消息失败（不影响主业务）: orderId={}, error={}", orderId, e.getMessage());
        }

        // 6. 生成收银台 URL
        String payUrl = buildPayUrl(orderId);

        return CreateOrderResponse.builder()
                .orderId(orderId)
                .merchantOrderNo(request.getMerchantOrderNo())
                .merchantId(request.getMerchantId())
                .payUrl(payUrl)
                .amount(request.getAmount())
                .currency(order.getCurrency())
                .expireTime(expireTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .status(Order.STATUS_CREATED)
                .build();
    }

    @Override
    public OrderDetailResponse getOrderDetail(String orderId) {
        // 1. 先查 Redis 缓存
        Order order = orderCacheService.getOrderWithFallback(orderId);
        if (order == null) {
            throw new BizException(6001, "订单不存在: " + orderId);
        }
        return buildOrderDetailResponse(order);
    }

    @Override
    public CashierResponse getCashierInfo(String orderId, String clientType) {
        // 1. 先查 Redis 缓存
        Order order = orderCacheService.getOrderWithFallback(orderId);
        if (order == null) {
            throw new BizException(6001, "订单不存在: " + orderId);
        }

        if (Order.STATUS_PAID.equals(order.getStatus())) {
            return CashierResponse.builder()
                    .orderId(order.getOrderId())
                    .merchantName("商户-" + order.getMerchantId())
                    .subject(order.getSubject())
                    .body(order.getBody())
                    .createdAt(order.getCreatedAt() != null
                            ? order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                    .amount(order.getAmount())
                    .currency(order.getCurrency())
                    .expireTime(order.getExpireTime() != null
                            ? order.getExpireTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                    .status(order.getStatus())
                    .successUrl(order.getSuccessUrl())
                    .failUrl(order.getFailUrl())
                    .build();
        }

        if (Order.STATUS_EXPIRED.equals(order.getStatus()) ||
                (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime()))) {
            throw new BizException(6002, "订单已过期: " + orderId);
        }

        if (!Order.STATUS_CREATED.equals(order.getStatus()) &&
                !Order.STATUS_PAYING.equals(order.getStatus())) {
            throw new BizException(6003, "订单状态异常: " + order.getStatus());
        }

        return CashierResponse.builder()
                .orderId(order.getOrderId())
                .merchantName("商户-" + order.getMerchantId())
                .subject(order.getSubject())
                .body(order.getBody())
                .createdAt(order.getCreatedAt() != null
                        ? order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .amount(order.getAmount())
                .currency(order.getCurrency())
                .expireTime(order.getExpireTime() != null
                        ? order.getExpireTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null)
                .status(order.getStatus())
                .paymentMethods(resolvePaymentMethods(order, clientType))
                .successUrl(order.getSuccessUrl())
                .failUrl(order.getFailUrl())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOrderStatus(String orderId, String newStatus, Long payAmount) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setStatus(newStatus);
        if (payAmount != null) {
            order.setPayAmount(payAmount);
        }
        if (Order.STATUS_PAID.equals(newStatus)) {
            order.setPayTime(LocalDateTime.now());
        }
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单状态更新: orderId={}, status={}", orderId, newStatus);

        // 更新缓存
        try {
            orderCacheService.evictOrder(orderId);
            // 重新查DB并回填（确保缓存数据最新）
            Order updatedOrder = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
            if (updatedOrder != null) {
                orderCacheService.cacheOrder(orderId, updatedOrder);
            }
        } catch (Exception e) {
            log.warn("Redis缓存同步失败（不影响主业务）: orderId={}, error={}", orderId, e.getMessage());
        }

        // 支付成功/失败 → 发送 MQ 商户回调消息
        if (Order.STATUS_PAID.equals(newStatus) || Order.STATUS_FAILED.equals(newStatus)) {
            try {
                orderMqProducer.sendPaymentResultNotify(orderId, newStatus, null);
            } catch (Exception e) {
                log.warn("MQ发送商户回调消息失败: orderId={}, error={}", orderId, e.getMessage());
            }
        }
    }

    @Override
    public OrderDetailResponse getOrderByMerchant(String orderId, String merchantId) {
        // 1. 优先从 Redis 缓存查
        Order order = orderCacheService.getOrderWithFallback(orderId);

        // 2. 校验订单存在
        if (order == null) {
            return null;
        }

        // 3. 校验 merchantId 匹配
        if (!merchantId.equals(order.getMerchantId())) {
            log.warn("商户查询订单，merchantId不匹配: orderId={}, requestMerchantId={}, actualMerchantId={}",
                    orderId, merchantId, order.getMerchantId());
            return null;
        }

        return buildOrderDetailResponse(order);
    }

    // ==================== 私有方法 ====================

    private OrderDetailResponse buildOrderDetailResponse(Order order) {
        List<Payment> payments = paymentMapper.selectList(
                new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, order.getOrderId()));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        List<OrderDetailResponse.PaymentRecordDTO> paymentDTOs = new ArrayList<>();
        for (Payment p : payments) {
            paymentDTOs.add(OrderDetailResponse.PaymentRecordDTO.builder()
                    .paymentId(p.getPaymentId())
                    .payChannel(p.getPayChannel())
                    .payMethod(p.getPayMethod())
                    .amount(p.getAmount())
                    .status(p.getStatus())
                    .channelTransactionId(p.getChannelTransactionId())
                    .createdAt(p.getCreatedAt() != null ? p.getCreatedAt().format(fmt) : null)
                    .updatedAt(p.getUpdatedAt() != null ? p.getUpdatedAt().format(fmt) : null)
                    .build());
        }

        return OrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .merchantId(order.getMerchantId())
                .merchantOrderNo(order.getMerchantOrderNo())
                .amount(order.getAmount())
                .payAmount(order.getPayAmount())
                .currency(order.getCurrency())
                .subject(order.getSubject())
                .body(order.getBody())
                .attach(order.getAttach())
                .channel(order.getChannel())
                .status(order.getStatus())
                .expireTime(order.getExpireTime() != null ? order.getExpireTime().format(fmt) : null)
                .payTime(order.getPayTime() != null ? order.getPayTime().format(fmt) : null)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().format(fmt) : null)
                .payments(paymentDTOs)
                .build();
    }

    private String buildPayUrl(String orderId) {
        String baseUrl = properties.getCashier().getBaseUrl();
        String sig = SignUtils.sign(properties.getSignature().getSecret(), orderId);
        return baseUrl + "/cashier/" + orderId + "?sig=" + sig;
    }

    private List<PaymentMethodDTO> resolvePaymentMethods(Order order, String clientType) {
        String normalized = normalizeClientType(clientType);
        List<AdminPaymentConfigClient.AdminPaymentMethodItem> items =
                adminPaymentConfigClient.fetchPaymentMethods(order.getMerchantId(), order.getChannel());
        if (!items.isEmpty()) {
            List<PaymentMethodDTO> fromAdmin = items.stream()
                    .filter(i -> i.visibleForClient(normalized))
                    .sorted(Comparator.comparingInt(AdminPaymentConfigClient.AdminPaymentMethodItem::priority).reversed())
                    .map(this::toPaymentMethodDtoFromAdmin)
                    .toList();
            if (!fromAdmin.isEmpty()) {
                return fromAdmin;
            }
        }
        List<PaymentMethodDTO> fallback = buildFallbackPaymentMethods(order.getChannel());
        return filterFallbackPaymentMethodsByClient(fallback, normalized);
    }

    private static String normalizeClientType(String clientType) {
        if (clientType == null || clientType.isBlank()) {
            return "";
        }
        return clientType.trim().toUpperCase();
    }

    private PaymentMethodDTO toPaymentMethodDtoFromAdmin(AdminPaymentConfigClient.AdminPaymentMethodItem item) {
        String code = item.methodCode();
        return PaymentMethodDTO.builder()
                .code(code)
                .name(item.methodName())
                .description(item.description() != null ? item.description() : "")
                .icon(iconForPaymentMethodCode(code))
                .recommended(isRecommendedPaymentMethod(code))
                .build();
    }

    private List<PaymentMethodDTO> filterFallbackPaymentMethodsByClient(List<PaymentMethodDTO> list,
                                                                        String normalizedClient) {
        if (normalizedClient == null || normalizedClient.isEmpty()) {
            return list;
        }
        return list.stream()
                .filter(d -> fallbackAllowsClient(d.getCode(), normalizedClient))
                .toList();
    }

    /**
     * 管理端不可用时，内置列表按典型终端归属过滤。
     */
    private boolean fallbackAllowsClient(String code, String client) {
        if (code == null) {
            return true;
        }
        if (Payment.METHOD_WECHAT_NATIVE.equals(code)) {
            return "PC".equals(client);
        }
        if (Payment.METHOD_WECHAT_H5.equals(code)) {
            return "H5".equals(client) || "APP".equals(client);
        }
        if (Payment.METHOD_WECHAT_APP.equals(code)) {
            return "APP".equals(client);
        }
        if (Payment.METHOD_WECHAT_JSAPI.equals(code) || Payment.METHOD_WECHAT_MINI.equals(code)) {
            return "H5".equals(client) || "APP".equals(client);
        }
        if (Payment.METHOD_WECHAT_MICROPAY.equals(code) || Payment.METHOD_ALIPAY_FACE.equals(code)) {
            return "PC".equals(client) || "H5".equals(client) || "APP".equals(client);
        }
        if (Payment.METHOD_UNION_H5.equals(code)) {
            return "H5".equals(client) || "APP".equals(client);
        }
        if (Payment.METHOD_ALIPAY_WAP.equals(code)) {
            return "H5".equals(client) || "APP".equals(client);
        }
        if (Payment.METHOD_ALIPAY_APP.equals(code)) {
            return "APP".equals(client);
        }
        if (Payment.METHOD_BANK_CARD.equals(code)) {
            return "PC".equals(client) || "H5".equals(client);
        }
        return true;
    }

    private boolean isRecommendedPaymentMethod(String code) {
        if (code == null) {
            return false;
        }
        return Payment.METHOD_WECHAT_APP.equals(code)
                || Payment.METHOD_ALIPAY_APP.equals(code)
                || Payment.METHOD_BANK_CARD.equals(code);
    }

    private String iconForPaymentMethodCode(String code) {
        if (code == null) {
            return "/icons/default-pay.png";
        }
        return switch (code) {
            case Payment.METHOD_WECHAT_APP -> "/icons/wechat.png";
            case Payment.METHOD_WECHAT_NATIVE -> "/icons/wechat-qr.png";
            case Payment.METHOD_WECHAT_H5 -> "/icons/wechat-h5.png";
            case Payment.METHOD_WECHAT_JSAPI, Payment.METHOD_WECHAT_MINI -> "/icons/wechat.png";
            case Payment.METHOD_WECHAT_MICROPAY -> "/icons/wechat-qr.png";
            case Payment.METHOD_ALIPAY_APP -> "/icons/alipay.png";
            case Payment.METHOD_ALIPAY_WAP -> "/icons/alipay-wap.png";
            case Payment.METHOD_BANK_CARD, Payment.METHOD_UNION_H5 -> "/icons/bank.png";
            case Payment.METHOD_ALIPAY_FACE -> "/icons/alipay.png";
            default -> "/icons/default-pay.png";
        };
    }

    private List<PaymentMethodDTO> buildFallbackPaymentMethods(String channel) {
        List<PaymentMethodDTO> methods = new ArrayList<>();
        if (Order.CHANNEL_WECHAT_PAY.equals(channel)) {
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_WECHAT_APP).name("微信支付").icon(iconForPaymentMethodCode(Payment.METHOD_WECHAT_APP))
                    .description("微信支付安全便捷").recommended(true).build());
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_WECHAT_NATIVE).name("微信扫码").icon(iconForPaymentMethodCode(Payment.METHOD_WECHAT_NATIVE))
                    .description("扫码支付").recommended(false).build());
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_WECHAT_H5).name("微信H5支付").icon(iconForPaymentMethodCode(Payment.METHOD_WECHAT_H5))
                    .description("网页支付").recommended(false).build());
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_WECHAT_JSAPI).name("微信JSAPI").icon(iconForPaymentMethodCode(Payment.METHOD_WECHAT_JSAPI))
                    .description("公众号内支付").recommended(false).build());
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_WECHAT_MINI).name("微信小程序").icon(iconForPaymentMethodCode(Payment.METHOD_WECHAT_MINI))
                    .description("小程序支付").recommended(false).build());
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_WECHAT_MICROPAY).name("微信付款码").icon(iconForPaymentMethodCode(Payment.METHOD_WECHAT_MICROPAY))
                    .description("被扫/条码").recommended(false).build());
        } else if (Order.CHANNEL_ALIPAY.equals(channel)) {
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_ALIPAY_APP).name("支付宝").icon(iconForPaymentMethodCode(Payment.METHOD_ALIPAY_APP))
                    .description("支付宝安全支付").recommended(true).build());
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_ALIPAY_WAP).name("支付宝WAP").icon(iconForPaymentMethodCode(Payment.METHOD_ALIPAY_WAP))
                    .description("网页支付").recommended(false).build());
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_ALIPAY_FACE).name("支付宝条码").icon(iconForPaymentMethodCode(Payment.METHOD_ALIPAY_FACE))
                    .description("付款码/当面付").recommended(false).build());
        } else if (Order.CHANNEL_UNION_PAY.equals(channel)) {
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_UNION_H5).name("云闪付H5").icon(iconForPaymentMethodCode(Payment.METHOD_UNION_H5))
                    .description("银联在线（占位跳转）").recommended(true).build());
            methods.add(PaymentMethodDTO.builder()
                    .code(Payment.METHOD_BANK_CARD).name("银行卡支付").icon(iconForPaymentMethodCode(Payment.METHOD_BANK_CARD))
                    .description("支持各大银行").recommended(false).build());
        }
        return methods;
    }
}
