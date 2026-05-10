package com.payflow.payment.wechat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * 微信支付 API v3 通用 HTTP 客户端（RSA 签名）。
 */
@Slf4j
@Component
public class WxPayV3HttpClient {

    private static final String API_BASE = "https://api.mch.weixin.qq.com";

    private final HttpClient httpClient;

    public WxPayV3HttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * POST 请求并返回响应体（HTTP 200/201）。
     *
     * @param apiLabel 日志用标签（如 Native/JSAPI）
     */
    public String postJson(WxPayAccountConfig config, String path, String requestBody, String apiLabel) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String nonceStr = StrUtil.uuid().replace("-", "");
            String signStr = buildSignature(config, "POST", path, timestamp, nonceStr, requestBody);
            String authorization = buildAuthorization(config, timestamp, nonceStr, signStr);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + path))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", authorization)
                    .header("User-Agent", "PayFlow-Cashier/1.0")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            int statusCode = response.statusCode();
            String body = response.body();
            if (statusCode != 200 && statusCode != 201) {
                log.error("微信API请求失败: label={}, path={}, status={}, body={}", apiLabel, path, statusCode, body);
                JSONObject errResp = JSONUtil.parseObj(body);
                String errMsg = errResp.getStr("message", "请求失败");
                throw new BizException(6005, "微信支付API错误(" + apiLabel + "): " + errMsg);
            }
            return body;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信API请求异常: label={}, path={}", apiLabel, path, e);
            throw new BizException(6005, "微信支付API异常(" + apiLabel + ")", e);
        }
    }

    /**
     * GET 请求并返回响应体（HTTP 200/201）。
     */
    public String get(WxPayAccountConfig config, String pathWithQuery, String apiLabel) {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String nonceStr = StrUtil.uuid().replace("-", "");
            String signStr = buildSignature(config, "GET", pathWithQuery, timestamp, nonceStr, "");
            String authorization = buildAuthorization(config, timestamp, nonceStr, signStr);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + pathWithQuery))
                    .header("Accept", "application/json")
                    .header("Authorization", authorization)
                    .header("User-Agent", "PayFlow-Cashier/1.0")
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            int statusCode = response.statusCode();
            String body = response.body();
            if (statusCode != 200 && statusCode != 201) {
                log.error("微信API GET失败: label={}, path={}, status={}, body={}", apiLabel, pathWithQuery, statusCode, body);
                throw new BizException(6005, "微信支付查单API错误: HTTP " + statusCode);
            }
            return body;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信API GET异常: label={}, path={}", apiLabel, pathWithQuery, e);
            throw new BizException(6005, "微信支付查单异常", e);
        }
    }

    private String buildSignature(WxPayAccountConfig config, String method,
                                  String path, long timestamp, String nonce,
                                  String body) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(config.getPrivateKeyObj());
            String signStr = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
            sig.update(signStr.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (NoSuchAlgorithmException | InvalidKeyException | java.security.SignatureException e) {
            throw new RuntimeException("签名算法异常", e);
        }
    }

    private String buildAuthorization(WxPayAccountConfig config, long timestamp,
                                      String nonce, String signature) {
        return "WXAUTH appId=\"" + config.appId + "\", "
                + "nonceStr=\"" + nonce + "\", "
                + "timestamp=\"" + timestamp + "\", "
                + "signature=\"" + signature + "\"";
    }
}
