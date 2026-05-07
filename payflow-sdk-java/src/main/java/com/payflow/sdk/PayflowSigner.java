package com.payflow.sdk;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * PayFlow 商户请求签名（HMAC-SHA256），与收银台 {@code MerchantSignatureInterceptor} 规则对齐的最小实现。
 */
public final class PayflowSigner {

    private PayflowSigner() {
    }

    /**
     * @param payload 待签名字符串（与网关文档一致：方法+路径+时间戳+body 等拼接规则）
     * @param secret  商户密钥
     */
    public static String hmacSha256Hex(String payload, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(raw);
    }
}
