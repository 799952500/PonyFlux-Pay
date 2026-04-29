package com.payflow.cashier.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

/**
 * 商户签名工具类
 * <p>
 * 签名算法：
 * 签名原文 = HTTP方法 + "\n" + 请求路径 + "\n" + queryString(按字典序排序) + "\n" + 时间戳
 * 签名 = HMAC-SHA256(签名原文, appSecret) → hex小写
 *
 * @author PayFlow Team
 */
@Slf4j
public final class MerchantSignatureUtil {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String LINE = "\n";

    private MerchantSignatureUtil() {
    }

    /**
     * 生成签名
     *
     * @param method      HTTP方法，如 GET、POST
     * @param path        请求路径，如 /api/v1/merchant/orders/ORD123
     * @param queryString 已按字典序排列的queryString，如 merchantId=M10001&timestamp=1713081600
     * @param timestamp   Unix时间戳（秒）
     * @param appSecret   商户密钥
     * @return 签名（hex小写）
     */
    public static String sign(String method, String path, String queryString, long timestamp, String appSecret) {
        String signStr = buildSignString(method, path, queryString, timestamp);
        return hmacSha256Hex(signStr, appSecret);
    }

    /**
     * 验证签名
     *
     * @param method      HTTP方法
     * @param path        请求路径
     * @param queryString 已按字典序排列的queryString
     * @param timestamp   Unix时间戳（秒）
     * @param appSecret   商户密钥
     * @param signature   待验证签名（hex小写）
     * @return true=验签通过
     */
    public static boolean verify(String method, String path, String queryString,
                                 long timestamp, String appSecret, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String expected = sign(method, path, queryString, timestamp, appSecret);
        // 使用恒定时间比较防止时序攻击
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 从 HttpServletRequest 中提取queryString，并按字典序排列
     * <p>
     * 注意：HttpServletRequest.getQueryString() 返回的是原始顺序，
     * 此方法将其转换为按参数名字典序排列的字符串。
     *
     * @param request HTTP请求
     * @return 按字典序排列的queryString，如 merchantId=M10001&timestamp=1713081600
     *                      如果没有参数则返回空字符串 ""
     */
    public static String buildQueryStringSorted(HttpServletRequest request) {
        Map<String, String> params = new TreeMap<>();
        Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String name = paramNames.nextElement();
            String value = request.getParameter(name);
            params.put(name, value);
        }
        if (params.isEmpty()) {
            return "";
        }
        // 按字典序拼接：key1=value1&key2=value2
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(k).append("=").append(v == null ? "" : v);
        });
        return sb.toString();
    }

    /**
     * 构造签名原文
     */
    private static String buildSignString(String method, String path, String queryString, long timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(LINE);
        sb.append(path).append(LINE);
        sb.append(queryString == null ? "" : queryString).append(LINE);
        sb.append(timestamp);
        return sb.toString();
    }

    /**
     * HMAC-SHA256 签名，返回 hex 小写（商户 HTTP 请求签、回调体签等共用）
     */
    public static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("签名算法异常", e);
            throw new RuntimeException("签名算法异常", e);
        }
    }

    /**
     * byte[] 转 hex 小写字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int v = b & 0xFF;
            if (v < 16) {
                hex.append('0');
            }
            hex.append(Integer.toHexString(v));
        }
        return hex.toString();
    }
}
