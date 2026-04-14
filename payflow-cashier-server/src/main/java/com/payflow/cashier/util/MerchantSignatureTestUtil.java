package com.payflow.cashier.util;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

/**
 * 商户签名测试工具类
 * <p>
 * 用于生成和验证商户签名，方便测试人员验证签名算法是否正确。
 * <p>
 * 运行方式：
 * 直接运行 main 方法，查看签名原文和签名结果的打印输出。
 * <p>
 * 代码中实际使用的是 MerchantSignatureUtil，测试时请参考本类的签名逻辑。
 *
 * @author PayFlow Team
 */
public class MerchantSignatureTestUtil {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String LINE = "\n";

    public static void main(String[] args) {
        System.out.println("=".repeat(70));
        System.out.println("商户签名测试工具");
        System.out.println("=".repeat(70));

        // ── 测试用例 1：标准 GET 请求（带 query 参数）────────────────────────
        testCase(
                "M2024040001",
                "4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f",
                "GET",
                "/api/v1/merchant/orders/ORD202404140001",
                createParams("merchantId", "M2024040001", "timestamp", "1713081600"),
                1713081600
        );

        // ── 测试用例 2：GET 请求（无 query 参数）────────────────────────────
        testCase(
                "M2024040001",
                "4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f",
                "GET",
                "/api/v1/merchant/orders/list",
                null,
                System.currentTimeMillis() / 1000
        );

        // ── 测试用例 3：POST 请求（带 JSON body，queryString 来自 URL）────
        testCase(
                "M2024040001",
                "4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f",
                "POST",
                "/api/v1/merchant/refund",
                createParams("orderId", "PO123456", "timestamp", "1713081600"),
                1713081600
        );

        // ── 测试用例 4：多参数（验证字典序排序）─────────────────────────────
        testCase(
                "M2024040001",
                "4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f",
                "GET",
                "/api/v1/merchant/orders/search",
                createParams("status", "PAID", "merchantId", "M2024040001", "timestamp", "1713081600"),
                1713081600
        );

        // ── 测试用例 5：签名验证（使用上面生成的签名）─────────────────────
        System.out.println("-".repeat(70));
        System.out.println("签名验证测试（使用用例1生成的签名）");
        System.out.println("-".repeat(70));

        String method = "GET";
        String path = "/api/v1/merchant/orders/ORD202404140001";
        String queryString = buildQueryStringSorted(createParams("merchantId", "M2024040001", "timestamp", "1713081600"));
        long timestamp = 1713081600;
        String appSecret = "4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f";

        String signStr = buildSignString(method, path, queryString, timestamp);
        String signature = hmacSha256Hex(signStr, appSecret);

        boolean valid = verifySignature(method, path, queryString, timestamp, appSecret, signature);
        System.out.println("签名原文:");
        System.out.println(signStr);
        System.out.println();
        System.out.println("签名结果: " + signature);
        System.out.println("验签结果: " + (valid ? "✅ 通过" : "❌ 失败"));

        // ── 测试用例 6：签名验证失败（篡改参数）────────────────────────────
        System.out.println();
        System.out.println("-".repeat(70));
        System.out.println("签名验证失败测试（篡改 merchantId）");
        System.out.println("-".repeat(70));
        String tamperedQueryString = buildQueryStringSorted(createParams("merchantId", "M9999999", "timestamp", "1713081600"));
        boolean tamperedValid = verifySignature(method, path, tamperedQueryString, timestamp, appSecret, signature);
        System.out.println("篡改后验签结果: " + (tamperedValid ? "✅ 通过" : "❌ 失败（预期）"));

        // ── curl 示例输出 ────────────────────────────────────────────────
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("curl 调用示例（用例1）");
        System.out.println("=".repeat(70));
        String curlSign = signature;
        String curlTimestamp = "1713081600";
        String curlMerchantId = "M2024040001";
        System.out.println("curl -X GET \\");
        System.out.println("  'http://localhost:3002/api/v1/merchant/orders/ORD202404140001?merchantId=M2024040001&timestamp=" + curlTimestamp + "' \\");
        System.out.println("  -H 'X-Merchant-Id: " + curlMerchantId + "' \\");
        System.out.println("  -H 'X-Timestamp: " + curlTimestamp + "' \\");
        System.out.println("  -H 'X-Sign: " + curlSign + "'");

        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("Java 调用示例（用例1）");
        System.out.println("=".repeat(70));
        System.out.println("// 签名原文构造");
        System.out.println("String method = \"GET\";");
        System.out.println("String path = \"/api/v1/merchant/orders/ORD202404140001\";");
        System.out.println("String queryString = \"merchantId=M2024040001&timestamp=" + curlTimestamp + "\";  // 必须按字典序排列");
        System.out.println("long timestamp = " + curlTimestamp + "L;");
        System.out.println("String appSecret = \"4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f\";");
        System.out.println();
        System.out.println("// 生成签名");
        System.out.println("String sign = MerchantSignatureUtil.sign(method, path, queryString, timestamp, appSecret);");
        System.out.println("// sign = \"" + signature + "\"");
        System.out.println();
        System.out.println("// 发送请求时带上三个请求头");
        System.out.println("// X-Merchant-Id: " + curlMerchantId);
        System.out.println("// X-Timestamp: " + curlTimestamp);
        System.out.println("// X-Sign: " + curlSign);
    }

    /**
     * 执行单个测试用例
     */
    private static void testCase(String merchantId, String appSecret,
                                   String method, String path,
                                   Map<String, String> params, long timestamp) {
        System.out.println("-".repeat(70));
        System.out.println("测试用例: " + method + " " + path);
        System.out.println("商户号: " + merchantId);
        System.out.println("参数: " + (params == null ? "无" : params));
        System.out.println("时间戳: " + timestamp);
        System.out.println("-".repeat(70));

        String queryString = params == null ? "" : buildQueryStringSorted(params);
        String signStr = buildSignString(method, path, queryString, timestamp);
        String signature = hmacSha256Hex(signStr, appSecret);

        System.out.println("排序后 queryString: " + queryString);
        System.out.println("签名原文:");
        System.out.println(signStr);
        System.out.println("签名结果 (hex): " + signature);
        System.out.println();
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
     * 验证签名（恒定时间比较）
     */
    private static boolean verifySignature(String method, String path, String queryString,
                                           long timestamp, String appSecret, String signature) {
        if (signature == null || signature.isBlank()) {
            return false;
        }
        String expected = hmacSha256Hex(buildSignString(method, path, queryString, timestamp), appSecret);
        return MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                signature.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    /**
     * HMAC-SHA256 签名，返回 hex 小写
     */
    private static String hmacSha256Hex(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance(ALGORITHM);
            javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return bytesToHex(hmacBytes);
        } catch (Exception e) {
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

    /**
     * 按字典序拼接参数
     */
    private static String buildQueryStringSorted(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }
        Map<String, String> sorted = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(k).append("=").append(v == null ? "" : v);
        });
        return sb.toString();
    }

    /**
     * 快速构建参数 Map（可变参数，key1, value1, key2, value2...）
     */
    private static Map<String, String> createParams(String... kv) {
        Map<String, String> map = new TreeMap<>();
        for (int i = 0; i < kv.length - 1; i += 2) {
            map.put(kv[i], kv[i + 1]);
        }
        return map;
    }
}
