package com.payflow.cashier.sdk.alipay;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.PayChannelAccount;
import com.payflow.cashier.mapper.PayChannelAccountMapper;
import com.payflow.cashier.service.PayNotifyService;
import com.payflow.payment.core.NotifyResult;
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
 * 支付宝回调通知解析辅助组件。
 * <p>
 * 封装支付宝 RSA-SHA256 验签和处理逻辑，供所有支付宝策略复用。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AliPayNotifyHelper {

    private final PayNotifyService payNotifyService;
    private final PayChannelAccountMapper payChannelAccountMapper;

    /** 支付宝签名算法 */
    private static final String ALIPAY_SIGN_ALGORITHM = "SHA256withRSA";

    /**
     * 解析并处理支付宝异步通知。
     *
     * @param params 通知参数（form-post key-value）
     * @return 通知解析结果
     */
    public NotifyResult parseNotify(Map<String, String> params) {
        try {
            // 1. 提取关键参数
            String tradeStatus = params.get("trade_status");
            String outTradeNo = params.get("out_trade_no"); // 商户订单号
            String tradeNo = params.get("trade_no"); // 支付宝交易号
            String totalAmount = params.get("total_amount");
            String notifyId = params.get("notify_id");
            String sign = params.get("sign");
            String appId = params.get("app_id");
            String signType = params.get("sign_type");

            log.info("支付宝回调解析: outTradeNo={}, tradeNo={}, tradeStatus={}, appId={}",
                    outTradeNo, tradeNo, tradeStatus, appId);

            // 2. 签名非空检查
            if (StrUtil.isBlank(sign)) {
                log.error("支付宝回调验签失败：签名为空");
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("签名为空")
                        .aliReply("fail")
                        .build();
            }

            // 3. RSA-SHA256 验签
            if (!verifyAlipaySign(params, sign, signType, appId)) {
                log.error("支付宝回调验签失败：签名不匹配, outTradeNo={}", outTradeNo);
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("签名验证失败")
                        .aliReply("fail")
                        .build();
            }

            // 4. 根据交易状态处理
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                payNotifyService.handlePaymentSuccess(outTradeNo, tradeNo);
                return NotifyResult.builder()
                        .success(true)
                        .tradeNo(tradeNo)
                        .outTradeNo(outTradeNo)
                        .aliReply("success")
                        .build();
            } else if ("WAIT_BUYER_PAY".equals(tradeStatus)) {
                log.info("支付宝回调：等待买家付款，忽略: outTradeNo={}", outTradeNo);
                return NotifyResult.builder()
                        .success(true)
                        .aliReply("success")
                        .build();
            } else {
                log.warn("支付宝回调：交易未成功: outTradeNo={}, status={}", outTradeNo, tradeStatus);
                return NotifyResult.builder()
                        .success(false)
                        .errorMsg("交易未成功: " + tradeStatus)
                        .aliReply("fail")
                        .build();
            }

        } catch (Exception e) {
            log.error("处理支付宝回调异常", e);
            return NotifyResult.builder()
                    .success(false)
                    .errorMsg(e.getMessage())
                    .aliReply("fail")
                    .build();
        }
    }

    /**
     * 验证支付宝回调 RSA-SHA256 签名。
     *
     * <p>签名流程：
     * <ol>
     *   <li>根据 app_id 查找对应的渠道账户，获取 alipayPublicKey</li>
     *   <li>移除 sign 和 sign_type 字段</li>
     *   <li>剩余字段按 key 字母序升序排列</li>
     *   <li>拼接为 "key1=value1&key2=value2&..." 格式</li>
     *   <li>使用 SHA256withRSA 算法验签</li>
     * </ol>
     *
     * @param params   回调参数
     * @param sign     Base64 编码的签名
     * @param signType 签名类型（RSA2）
     * @param appId    支付宝应用 ID
     * @return true=验签通过
     */
    private boolean verifyAlipaySign(Map<String, String> params, String sign,
                                     String signType, String appId) {
        try {
            // 获取支付宝公钥
            String alipayPublicKey = findAlipayPublicKey(appId);
            if (alipayPublicKey == null) {
                log.error("未找到匹配的支付宝公钥: appId={}", appId);
                return false;
            }

            // 构建待签名字符串
            String signContent = buildAlipaySignContent(params);
            log.debug("支付宝验签原文: {}", signContent);

            // RSA-SHA256 验签
            byte[] signBytes = Base64.getDecoder().decode(sign);
            PublicKey publicKey = loadAlipayPublicKey(alipayPublicKey);
            Signature signature = Signature.getInstance(ALIPAY_SIGN_ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(signContent.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            boolean valid = signature.verify(signBytes);

            if (!valid) {
                log.error("支付宝回调签名不匹配: appId={}", appId);
            }
            return valid;

        } catch (Exception e) {
            log.error("支付宝验签异常: appId={}", appId, e);
            return false;
        }
    }

    /**
     * 根据 app_id 查找对应渠道账户中配置的 alipayPublicKey。
     *
     * @param appId 支付宝应用 ID（来自回调参数）
     * @return 支付宝公钥（PEM 格式，不含头尾），未找到返回 null
     */
    private String findAlipayPublicKey(String appId) {
        if (StrUtil.isBlank(appId)) {
            return null;
        }
        List<PayChannelAccount> accounts = payChannelAccountMapper.selectList(
                new LambdaQueryWrapper<PayChannelAccount>()
                        .eq(PayChannelAccount::getStatus, "ENABLED"));
        for (PayChannelAccount account : accounts) {
            try {
                JSONObject config = JSONUtil.parseObj(account.getChannelConfig());
                String cfgAppId = config.getStr("appId");
                if (appId.equals(cfgAppId)) {
                    return config.getStr("alipayPublicKey");
                }
            } catch (Exception e) {
                log.debug("解析渠道配置失败: accountCode={}", account.getAccountCode());
            }
        }
        return null;
    }

    /**
     * 构建支付宝待签名字符串。
     * <p>
     * 规则：移除 sign、sign_type 字段，其余按 key 字母序升序排列，
     * 拼接为 "key1=value1&key2=value2&..."。
     * </p>
     *
     * @param params 回调参数（含 sign）
     * @return 待签名字符串
     */
    private String buildAlipaySignContent(Map<String, String> params) {
        TreeMap<String, String> sorted = new TreeMap<>(params);
        sorted.remove("sign");
        sorted.remove("sign_type");
        StringBuilder sb = new StringBuilder(1024);
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(key).append('=').append(value);
        }
        return sb.toString();
    }

    /**
     * 从 PEM 格式公钥字符串加载 PublicKey。
     *
     * @param publicKeyPem 支付宝公钥（单行 Base64 或含头尾的 PEM 格式）
     * @return RSA PublicKey
     */
    private PublicKey loadAlipayPublicKey(String publicKeyPem) throws Exception {
        String pem = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}
