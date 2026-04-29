package com.payflow.cashier.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;

/**
 * 支付宝异步回调通知接收器。
 * <p>
 * 接收路径：POST /api/v1/callbacks/alipay
 * 支付宝将支付结果以 FORM POST 形式通知到此地址。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliPayNotifyController {

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /**
     * 接收支付宝异步通知。
     *
     * @param params 通知参数 Map
     * @return 成功返回 "success"，失败返回 "fail"
     */
    /**
     * 接收支付宝异步通知（直接入口，保留向后兼容）。
     * <p>
     * 建议优先使用统一入口 {@code POST /api/v1/callbacks}，由框架自动路由。
     * </p>
     */
    @PostMapping("/api/v1/callbacks/alipay")
    @ResponseBody
    public String handleAliPayNotify(@RequestParam Map<String, String> params) {
        log.info("收到支付宝回调: params={}", params);

        try {
            // 1. 提取关键参数
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no"); // 商户订单号
            String tradeNo = params.get("trade_no"); // 支付宝交易号
            String totalAmount = params.get("total_amount");
            String notifyId = params.get("notify_id");
            String sign = params.get("sign");

            log.info("支付宝回调解析: outTradeNo={}, tradeNo={}, tradeStatus={}",
                    outTradeNo, tradeNo, tradeStatus);

            // 2. 验签（重要！）
            // 实际生产环境应使用 AlipayConfigLoader 获取支付宝公钥验签
            // 这里先记录日志，后续实现
            if (StrUtil.isBlank(sign)) {
                log.error("支付宝回调验签失败：签名为空");
                return "fail";
            }

            // 3. 根据交易状态处理
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                handlePaymentSuccess(outTradeNo, tradeNo);
                return "success";
            } else if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
                log.info("支付宝回调：等待买家付款，忽略: outTradeNo={}", outTradeNo);
                return "success";
            } else {
                log.warn("支付宝回调：交易未成功: outTradeNo={}, status={}", outTradeNo, tradeStatus);
                return "fail";
            }

        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return "fail";
        }
    }

    /**
     * 处理支付成功。
     */
    private void handlePaymentSuccess(String orderId, String channelTransactionId) {
        // 更新 Payment 状态
        Payment payment = paymentMapper.selectOne(
                new LambdaQueryWrapper<Payment>()
                        .eq(Payment::getOrderId, orderId)
                        .eq(Payment::getStatus, Payment.STATUS_PROCESSING)
                        .orderByDesc(Payment::getCreatedAt)
                        .last("LIMIT 1"));
        if (payment == null) {
            log.warn("未找到待支付记录: orderId={}", orderId);
            return;
        }

        payment.setChannelTransactionId(channelTransactionId);
        payment.setStatus(Payment.STATUS_SUCCESS);
        payment.setUpdatedAt(java.time.LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 更新 Order 状态
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
        if (order != null) {
            orderService.updateOrderStatus(orderId, Order.STATUS_PAID, null);
        }

        log.info("支付宝支付完成: orderId={}, paymentId={}", orderId, payment.getPaymentId());
    }
}
