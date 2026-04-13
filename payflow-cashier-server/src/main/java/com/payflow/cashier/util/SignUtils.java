package com.payflow.cashier.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * 签名工具类
 * 支持 HMAC-SHA256 签名
 *
 * @author PayFlow Team
 */
public final class SignUtils {

    private static final String ALGORITHM = "HmacSHA256";

    private SignUtils() {
    }

    /**
     * 生成签名
     *
     * @param secret  签名密钥
     * @param signStr 签名原文
     * @return Base64 编码签名
     */
    public static String sign(String secret, String signStr) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("签名算法异常", e);
        }
    }

    /**
     * 验证签名
     *
     * @param secret    签名密钥
     * @param signStr   签名原文
     * @param signature 待验证签名
     * @return true=验签通过
     */
    public static boolean verify(String secret, String signStr, String signature) {
        return sign(secret, signStr).equals(signature);
    }

    /**
     * 生成订单号
     */
    public static String generateOrderId() {
        return "PO" + System.currentTimeMillis() + RandomUtil.randomNumbers(6);
    }

    /**
     * 生成支付记录ID
     */
    public static String generatePaymentId() {
        return "PAY" + System.currentTimeMillis() + RandomUtil.randomNumbers(6);
    }

    /**
     * 生成雪花ID
     */
    public static String snowflakeId() {
        return String.valueOf(IdUtil.getSnowflakeNextId());
    }
}
