package com.payflow.cashier.sdk.wxpay;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/**
 * 微信支付 APIv3 回调验签（平台证书 RSA + 时间戳防重放）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxPaySignatureVerifier {

    private static final String SIGN_ALGORITHM = "SHA256withRSA";
    private static final long MAX_TIMESTAMP_SKEW_SECONDS = 300L;

    private final WxPayPlatformCertCache platformCertCache;

    /**
     * 验证回调签名与时间戳。
     *
     * @return true 验签通过
     */
    public boolean verify(String serial, String signature, String timestamp, String nonce, String body) {
        if (isBlank(serial) || isBlank(signature) || isBlank(timestamp) || isBlank(nonce) || body == null) {
            log.warn("微信回调验签失败: 缺少必要头或 body");
            return false;
        }
        if (!isTimestampValid(timestamp)) {
            log.warn("微信回调验签失败: 时间戳超出窗口, timestamp={}", timestamp);
            return false;
        }
        PublicKey publicKey = platformCertCache.findBySerial(serial);
        if (publicKey == null) {
            log.warn("微信回调验签失败: 未配置平台证书, serial={}", serial);
            return false;
        }
        try {
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            Signature sig = Signature.getInstance(SIGN_ALGORITHM);
            sig.initVerify(publicKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            boolean ok = sig.verify(Base64.getDecoder().decode(signature.trim()));
            if (!ok) {
                log.warn("微信回调验签失败: 签名不匹配, serial={}", serial);
            }
            return ok;
        } catch (Exception e) {
            log.warn("微信回调验签异常: serial={}, error={}", serial, e.getMessage());
            return false;
        }
    }

    private static boolean isTimestampValid(String timestamp) {
        try {
            long ts = Long.parseLong(timestamp.trim());
            long now = System.currentTimeMillis() / 1000;
            return Math.abs(now - ts) <= MAX_TIMESTAMP_SKEW_SECONDS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
