package com.payflow.admin.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 本机 HTTP 冒烟测试（依赖你已启动 admin-server 与 cashier-server）。
 *
 * <p>覆盖：</p>
 * <ul>
 *     <li>admin captcha + login</li>
 *     <li>admin channel routing health</li>
 *     <li>admin onboarding list</li>
 *     <li>admin insights funnel</li>
 *     <li>admin reconcile anomalies（空数据也需 200）</li>
 *     <li>cashier public payment link</li>
 *     <li>cashier merchant signed create payment link（验签 + 限流链路）</li>
 * </ul>
 */
public class HttpSmokeRunnerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final String ADMIN_BASE = "http://127.0.0.1:3003/api/v1";
    private static final String CASHIER_BASE = "http://127.0.0.1:3002/api/v1";

    @Test
    void smoke() throws Exception {
        // 1) 基础可达
        assertStatus(ADMIN_BASE + "/admin/meta/features", 200);
        assertStatus("http://127.0.0.1:3002/api-docs", 200);

        // 2) cashier 公共 payment link（DB 由 LocalDbMigrateAndSmoke 写入）
        JsonNode publicLink = getJson(CASHIER_BASE + "/public/payment-links/LNKSMOKE01");
        assertEquals(0, publicLink.path("code").asInt());
        assertEquals("LNKSMOKE01", publicLink.path("data").path("linkId").asText());

        // 3) admin captcha + login
        JsonNode cap = getJson(ADMIN_BASE + "/admin/auth/captcha");
        String captchaId = cap.path("data").path("captchaId").asText(null);
        String question = cap.path("data").path("question").asText(null);
        assertNotNull(captchaId);
        assertNotNull(question);
        String captchaAnswer = solve(question);

        String loginBody = "{\"username\":\"admin\",\"password\":\"admin123\",\"captchaId\":\""
                + captchaId + "\",\"captchaAnswer\":\"" + captchaAnswer + "\"}";
        JsonNode login = postJson(ADMIN_BASE + "/admin/auth/login", loginBody, null);
        assertEquals(0, login.path("code").asInt(), login.toString());
        String token = login.path("data").path("token").asText(null);
        assertNotNull(token);

        String auth = "Bearer " + token;

        // 4) 智能路由健康度（accountCode=1001 是 admin/payment_accounts 示例；就算没统计也应返回 JSON）
        JsonNode health = getJson(ADMIN_BASE + "/admin/channel-routing/health?accountCode=1001", auth);
        assertTrue(health.has("accountCode") || health.has("error"), health.toString());

        // 5) KYB 进件列表（可能为空，200 即可）
        HttpResponse<String> kyb = get(ADMIN_BASE + "/admin/onboarding/applications", auth);
        assertEquals(200, kyb.statusCode(), kyb.body());

        // 6) 漏斗占位
        JsonNode funnel = getJson(ADMIN_BASE + "/admin/insights/funnel", auth);
        assertTrue(funnel.has("created") || funnel.has("note"), funnel.toString());

        // 7) 对账异常（没有任务也应 200）
        String billDate = LocalDate.now().toString();
        HttpResponse<String> anomalies = get(ADMIN_BASE + "/admin/reconcile/anomalies?billDate=" + billDate + "&page=1&size=5", auth);
        assertEquals(200, anomalies.statusCode(), anomalies.body());

        // 8) cashier 商户签名创建 Payment Link（验证验签/限流链路）
        String merchantId = "M2024040001";
        String appSecret = "4f3c2b1a0e9d8c7b6a5f4e3d2c1b0a9f";
        long ts = System.currentTimeMillis() / 1000;
        String path = "/api/v1/payment-links";
        String sign = signHmacSha256Hex("POST", path, "", ts, appSecret);
        String createLinkBody = "{\"title\":\"冒烟-创建链接\",\"amount\":100,\"currency\":\"CNY\"}";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:3002" + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("X-Merchant-Id", merchantId)
                .header("X-Timestamp", String.valueOf(ts))
                .header("X-Sign", sign)
                .POST(HttpRequest.BodyPublishers.ofString(createLinkBody, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> created = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, created.statusCode(), created.body());
        JsonNode createdJson = MAPPER.readTree(created.body());
        assertEquals(0, createdJson.path("code").asInt(), createdJson.toString());
        assertNotNull(createdJson.path("data").path("linkId").asText(null));
    }

    private static void assertStatus(String url, int expected) throws Exception {
        HttpResponse<String> r = get(url, null);
        assertEquals(expected, r.statusCode(), r.body());
    }

    private static HttpResponse<String> get(String url, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .GET();
        if (bearer != null && !bearer.isBlank()) {
            b.header("Authorization", bearer);
        }
        return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static JsonNode getJson(String url) throws Exception {
        return getJson(url, null);
    }

    private static JsonNode getJson(String url, String bearer) throws Exception {
        HttpResponse<String> r = get(url, bearer);
        assertEquals(200, r.statusCode(), r.body());
        return MAPPER.readTree(r.body());
    }

    private static JsonNode postJson(String url, String json, String bearer) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8));
        if (bearer != null && !bearer.isBlank()) {
            b.header("Authorization", bearer);
        }
        HttpResponse<String> r = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(200, r.statusCode(), r.body());
        return MAPPER.readTree(r.body());
    }

    private static String solve(String question) {
        // 示例： "6 + 1 = ?"
        Pattern p = Pattern.compile("(\\d+)\\s*\\+\\s*(\\d+)");
        Matcher m = p.matcher(question);
        if (!m.find()) {
            throw new IllegalArgumentException("无法解析验证码题目: " + question);
        }
        int a = Integer.parseInt(m.group(1));
        int b = Integer.parseInt(m.group(2));
        return String.valueOf(a + b);
    }

    private static String signHmacSha256Hex(String method, String path, String queryString, long timestamp, String secret) throws Exception {
        String signStr = method + "\n" + path + "\n" + (queryString == null ? "" : queryString) + "\n" + timestamp;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(signStr.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(raw);
    }
}

