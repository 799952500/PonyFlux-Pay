package com.payflow.cashier.sdk.wxpay;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.payment.core.NotifyResult;
import com.payflow.cashier.service.PayNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

/**
 * 微信支付回调通知解析辅助组件。
 * <p>
 * 封装 v3 AES-256-GCM 解密逻辑，供所有微信策略复用。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxPayNotifyHelper {

    private static final int GCM_TAG_LENGTH = 128; // bits
    private static final String AES_GCM_ALGO = "AES/GCM/NoPadding";

    private final PayNotifyService payNotifyService;

    /**
     * 解析并处理微信支付回调。
     *
     * @param serial    Wechatpay-Serial 请求头
     * @param signature Wechatpay-Signature 请求头
     * @param timestamp Wechatpay-Timestamp 请求头
     * @param nonce     Wechatpay-Nonce 请求头
     * @param body      回调 body（JSON）
     * @return 通知解析结果
     */
    public NotifyResult parseNotify(String serial, String signature,
                                     String timestamp, String nonce, String body) {
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
                return NotifyResult.builder()
                        .success(true)
                        .wxReply("SUCCESS")
                        .build();
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
                payNotifyService.handlePaymentSuccess(outTradeNo, transactionId);
            }

            return NotifyResult.builder()
                    .success(true)
                    .tradeNo(transactionId)
                    .outTradeNo(outTradeNo)
                    .wxReply("SUCCESS")
                    .build();

        } catch (Exception e) {
            log.error("处理微信支付回调异常", e);
            return NotifyResult.builder()
                    .success(false)
                    .errorMsg(e.getMessage())
                    .wxReply("FAIL")
                    .build();
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
     * 实际生产应从数据库 cashier_channel_accounts 的 channelConfig 字段读取。
     * </p>
     *
     * @return APIv3 密钥（32字节）
     */
    private byte[] getWxPayApiV3Key() {
        throw new UnsupportedOperationException(
                "请实现 getWxPayApiV3Key()，从数据库或配置中心获取 APIv3 密钥");
    }
}
