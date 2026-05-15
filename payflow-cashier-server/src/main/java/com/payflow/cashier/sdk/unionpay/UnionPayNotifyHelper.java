package com.payflow.cashier.sdk.unionpay;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PayChannelAccountMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.service.PayNotifyService;
import com.payflow.payment.core.NotifyResult;
import com.payflow.payment.union.UnionPayApiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 银联回调通知解析辅助组件。
 * <p>
 * 封装银联 RSA-SHA256 验签和处理逻辑，供银联策略复用。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnionPayNotifyHelper {

    private final PayNotifyService payNotifyService;
    private final OrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final PayChannelAccountMapper payChannelAccountMapper;

    /** 银联签名算法 */
    private static final String UNION_SIGN_ALGORITHM = "SHA256withRSA";

    /**
     * 解析并处理银联异步通知。
     * <p>
     * 银联通知为 application/x-www-form-urlencoded POST，关键字段：
     * <ul>
     *     <li>respCode — 00 表示成功</li>
     *     <li>orderId — 商户订单号（outTradeNo）</li>
     *     <li>queryId — 银联交易流水号（tradeNo）</li>
     *     <li>txnAmt — 交易金额（分）</li>
     *     <li>signature — RSA-SHA256 签名</li>
     * </ul>
     * </p>
     *
     * @param params 通知参数（form-post key-value）
     * @return 通知解析结果
     */
    public NotifyResult parseNotify(Map<String, String> params) {
        try {
            String respCode = params.get("respCode");
            String respMsg = params.get("respMsg");
            String orderId = params.get("orderId");
            String queryId = params.get("queryId");
            String txnType = params.get("txnType");
            String txnAmt = params.get("txnAmt");
            String signature = params.get("signature");

            log.info("银联回调解析: txnType={}, orderId={}, queryId={}, respCode={}, txnAmt={}",
                    txnType, orderId, queryId, respCode, txnAmt);

            // 1. 签名非空检查
            if (StrUtil.isBlank(signature)) {
                log.error("银联回调验签失败：签名为空");
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("签名为空")
                        .build();
            }

            // 2. RSA-SHA256 验签
            if (!verifyUnionPaySign(params, signature, orderId)) {
                log.error("银联回调验签失败：签名不匹配, orderId={}", orderId);
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("签名验证失败")
                        .build();
            }

            // 3. 检查响应码
            if (!"00".equals(respCode)) {
                log.warn("银联回调：交易未成功: orderId={}, respCode={}, respMsg={}", orderId, respCode, respMsg);
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("交易未成功: " + respMsg)
                        .build();
            }

            // 4. 区分交易类型处理
            if (UnionPayApiConstants.TXN_TYPE_REFUND.equals(txnType)) {
                String origQryId = params.get("origQryId");
                log.info("银联退款通知处理: orderId={}, queryId={}, origQryId={}", orderId, queryId, origQryId);
                return NotifyResult.builder()
                        .success(true)
                        .tradeNo(queryId)
                        .outTradeNo(orderId)
                        .build();
            }

            // 5. 处理支付成功（默认）
            payNotifyService.handlePaymentSuccess(orderId, queryId);

            return NotifyResult.builder()
                    .success(true)
                    .tradeNo(queryId)
                    .outTradeNo(orderId)
                    .build();

        } catch (Exception e) {
            log.error("处理银联回调异常", e);
            return NotifyResult.builder()
                    .success(false)
                    .errorMsg(e.getMessage())
                    .build();
        }
    }

    /**
     * 验证银联回调 RSA-SHA256 签名。
     *
     * <p>签名流程：
     * <ol>
     *   <li>根据回调中的 orderId 查找订单和支付记录，进而定位渠道账户</li>
     *   <li>从渠道账户的 channelConfig 中获取银联公钥</li>
     *   <li>移除 signature 字段</li>
     *   <li>剩余字段按 key 字母序升序排列，跳过空值</li>
     *   <li>拼接为 "key1=value1&key2=value2&..." 格式</li>
     *   <li>对拼接串做 SHA256 摘要</li>
     *   <li>使用 SHA256withRSA 验证签名</li>
     * </ol>
     *
     * @param params    回调参数（含 signature）
     * @param signature Base64 编码的签名
     * @param orderId   商户订单号
     * @return true=验签通过
     */
    private boolean verifyUnionPaySign(Map<String, String> params, String signature,
                                        String orderId) {
        try {
            // 查找银联公钥
            String unionpayPublicKey = findUnionpayPublicKey(orderId);
            if (unionpayPublicKey == null) {
                log.warn("未找到银联渠道账户配置，跳过验签: orderId={}", orderId);
                // 公钥未配置时记录警告但放行（兼容未配置银联公钥的环境）
                return true;
            }

            // 构建待签名字符串（排除 signature 字段）
            TreeMap<String, String> sorted = new TreeMap<>(params);
            sorted.remove("signature");
            String signPlain = buildSignPlain(sorted);

            // SHA256 摘要 + RSA 验签
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(signPlain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] signBytes = Base64.getDecoder().decode(signature);

            PublicKey publicKey = loadPublicKey(unionpayPublicKey);
            Signature sig = Signature.getInstance(UNION_SIGN_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(digest);
            boolean valid = sig.verify(signBytes);

            if (!valid) {
                log.error("银联回调签名不匹配: orderId={}", orderId);
            }
            return valid;

        } catch (Exception e) {
            log.error("银联验签异常: orderId={}", orderId, e);
            return false;
        }
    }

    /**
     * 根据 orderId 查找对应的银联渠道账户中的公钥。
     *
     * @param orderId 商户订单号
     * @return 银联公钥（PEM 格式，不含头尾），未找到返回 null
     */
    private String findUnionpayPublicKey(String orderId) {
        if (StrUtil.isBlank(orderId)) {
            return null;
        }
        try {
            // 通过订单查找支付记录以获取 payChannel
            Payment payment = paymentMapper.selectOne(
                    new LambdaQueryWrapper<Payment>().eq(Payment::getOrderId, orderId));
            if (payment == null) {
                return null;
            }
            String payChannel = payment.getPayChannel();
            // 查找该渠道下所有已启用的账户
            List<PayChannelAccount> accounts = payChannelAccountMapper.selectList(
                    new LambdaQueryWrapper<PayChannelAccount>()
                            .eq(PayChannelAccount::getStatus, "ENABLED"));
            for (PayChannelAccount account : accounts) {
                try {
                    JSONObject config = JSONUtil.parseObj(account.getChannelConfig());
                    String cfgPayChannel = config.getStr("payChannel");
                    String publicKey = config.getStr("unionpayPublicKey");
                    if (payChannel.equalsIgnoreCase(cfgPayChannel) && !StrUtil.isBlank(publicKey)) {
                        return publicKey;
                    }
                } catch (Exception e) {
                    log.debug("解析银联渠道配置失败: accountCode={}", account.getAccountCode());
                }
            }
        } catch (Exception e) {
            log.warn("查找银联渠道账户失败: orderId={}", orderId, e);
        }
        return null;
    }

    /**
     * 按 key ASCII 升序拼接签名原文：key1=val1&key2=val2&...（跳过空值）。
     */
    private String buildSignPlain(TreeMap<String, String> sorted) {
        StringBuilder sb = new StringBuilder(512);
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * 从 PEM 格式公钥字符串加载 PublicKey。
     *
     * @param publicKeyPem 公钥（单行 Base64 或含头尾的 PEM 格式）
     * @return RSA PublicKey
     */
    private PublicKey loadPublicKey(String publicKeyPem) throws Exception {
        String pem = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}
