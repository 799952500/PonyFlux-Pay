package com.payflow.cashier.controller;

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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Base64;

/**
 * 微信支付异步回调通知接收器。
 * <p>
 * 接收路径：POST /api/v1/callbacks/wxpay
 * 微信支付 v3 API 将支付结果以 JSON 形式 POST 到此地址。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxPayNotifyController {

    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final String AES_GCM_ALGO = "AES/GCM/NoPadding";

    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;
    private final OrderService orderService;

    /**
     * 接收微信支付结果通知。
     *
     * @param serial   微信平台证书序列号
     * @param signature 回调签名
     * @param timestamp 时间戳
     * @param nonce     随机字符串
     * @param body      通知内容（JSON）
     * @return 成功返回 "SUCCESS"，失败返回 "FAIL"
     */
    /**
     * 接收微信支付结果通知（直接入口，保留向后兼容）。
     * <p>
     * 建议优先使用统一入口 {@code POST /api/v1/callbacks}，由框架自动路由。
     * </p>
     */
    @PostMapping("/api/v1/callbacks/wxpay")
    @ResponseBody
    public String handleWxPayNotify(
            @RequestHeader("Wechatpay-Serial") String serial,
            @RequestHeader("Wechatpay-Signature") String signature,
            @RequestHeader("Wechatpay-Timestamp") String timestamp,
            @RequestHeader("Wechatpay-Nonce") String nonce,
            @RequestBody String body) {

        log.info("收到微信支付回调: serial={}, timestamp={}", serial, timestamp);

        try {
            // 1. 解析通知事件类型
            JSONObject notifyJson = JSONUtil.parseObj(body);
            String eventType = notifyJson.getStr("event_type");
            Object resourceRaw = notifyJson.get("resource");
        JSONObject resource = resourceRaw instanceof JSONObject
                ? (JSONObject) resourceRaw
                : JSONUtil.parseObj(resourceRaw.toString());

            if (!"TRANSACTION.SUCCESS".equals(eventType)) {
                log.info("忽略非成功事件: eventType={}", eventType);
                return "SUCCESS";
            }

            // 2. 解密 resource（AES-256-GCM）
            String plainText = decryptResource(resource);

            // 3. 解析支付结果
            JSONObject tradeState = JSONUtil.parseObj(plainText);
            String outTradeNo = tradeState.getStr("out_trade_no");
            String transactionId = tradeState.getStr("transaction_id");
            String tradeStateDoc = tradeState.getStr("trade_state");

            log.info("微信支付回调解析: outTradeNo={}, transactionId={}, tradeState={}",
                    outTradeNo, transactionId, tradeStateDoc);

            // 4. 处理支付成功
            if ("SUCCESS".equals(tradeStateDoc)) {
                handlePaymentSuccess(outTradeNo, transactionId);
            }

            return "SUCCESS";

        } catch (Exception e) {
            log.error("处理微信支付回调异常", e);
            return "FAIL";
        }
    }

    /**
     * 解密微信支付通知中的 resource 字段（AES-256-GCM）。
     *
     * @param resource 通知资源对象
     * @return 解密后的明文 JSON
     * @throws GeneralSecurityException 解密失败
     */
    private String decryptResource(JSONObject resource) throws GeneralSecurityException {
        String algorithm = resource.getStr("algorithm");
        String ciphertext = resource.getStr("ciphertext");
        String associatedData = resource.getStr("associated_data");
        String nonceStr = resource.getStr("nonce");

        if (!"AEAD_AES_256_GCM".equals(algorithm)) {
            throw new GeneralSecurityException("不支持的加密算法: " + algorithm);
        }

        // 获取 APIv3 密钥（实际生产应从数据库或配置中心读取）
        byte[] apiV3Key = getWxPayApiV3Key();
        byte[] nonce = nonceStr.getBytes(StandardCharsets.UTF_8);
        byte[] aad = associatedData.getBytes(StandardCharsets.UTF_8);
        byte[] cipherBytes = Base64.getDecoder().decode(ciphertext);

        SecretKeySpec keySpec = new SecretKeySpec(apiV3Key, "AES");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);

        Cipher cipher = Cipher.getInstance(AES_GCM_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);
        if (aad.length > 0) {
            cipher.updateAAD(aad);
        }

        byte[] plainBytes = cipher.doFinal(cipherBytes);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    /**
     * 获取微信支付 APIv3 密钥。
     * <p>
     * 实际生产应从数据库 cashier_channel_accounts 的 channelConfig 字段读取，
     * 或从独立配置中心获取。
     * </p>
     *
     * @return APIv3 密钥（32字节）
     */
    private byte[] getWxPayApiV3Key() {
        // TODO: 从数据库账户配置中获取 APIv3 密钥
        // 实际实现：根据通知内容中的 mchid 从 cashier_channel_accounts 读取
        // 这里抛异常，确保生产部署前必须实现
        throw new UnsupportedOperationException(
                "请实现 getWxPayApiV3Key()，从数据库或配置中心获取 APIv3 密钥");
    }

    /**
     * 处理支付成功。
     */
    private void handlePaymentSuccess(String orderId, String channelTransactionId) {
        // 查询 Payment 记录
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

        // 更新 Payment
        payment.setChannelTransactionId(channelTransactionId);
        payment.setStatus(Payment.STATUS_SUCCESS);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // 更新 Order
        Order order = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
        if (order != null) {
            orderService.updateOrderStatus(orderId, Order.STATUS_PAID, null);
        }

        log.info("支付完成: orderId={}, paymentId={}", orderId, payment.getPaymentId());
    }
}
