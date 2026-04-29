package com.payflow.payment.wechat;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信支付 App 支付处理器。
 * <p>
 * 调用微信支付 v3 API /v3/pay/transactions/app 接口，
 * 返回调起微信 App 所需的支付参数（appId, partnerId, prepayId, package, sign, nonceStr, timestamp）。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class WxPayAppHandler {

    /** 微信支付 API v3 base URL */
    private static final String API_BASE = "https://api.mch.weixin.qq.com";

    private final HttpClient httpClient;

    public WxPayAppHandler() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 创建 App 支付订单。
     *
     * @param orderId    商户订单号（用作 outTradeNo）
     * @param amount     订单金额（分）
     * @param description 商品描述
     * @param notifyUrl  异步通知地址
     * @param account    渠道账户配置
     * @return App 调起参数 Map，包含 appId, partnerId, prepayId, package, sign, nonceStr, timestamp
     */
    public Map<String, String> createAppOrder(String orderId, Long amount,
                                               String description, String notifyUrl,
                                               ChannelConfigHolder account) {
        WxPayAccountConfig config = WxPayConfigLoader.load(account);

        try {
            // 1. 调用统一下单 API
            JSONObject body = new JSONObject();
            body.set("mchid", config.mchId);
            body.set("out_trade_no", orderId);
            body.set("appid", config.appId);
            body.set("description", description);
            body.set("notify_url", notifyUrl);

            JSONObject amountJson = new JSONObject();
            amountJson.set("total", amount);
            amountJson.set("currency", "CNY");
            body.set("amount", amountJson);

            String requestBody = body.toString();
            String responseBody = executeV3Post(config, "/v3/pay/transactions/app", requestBody);

            JSONObject resp = JSONUtil.parseObj(responseBody);
            String prepayId = resp.getStr("prepay_id");

            if (prepayId == null || prepayId.isBlank()) {
                throw new BizException(6005, "微信App支付下单失败：未获取到 prepay_id");
            }

            log.info("微信App支付下单成功: orderId={}, prepayId={}", orderId, prepayId);

            // 2. 构建调起参数（App端调起微信支付需要签名）
            long timestamp = Instant.now().getEpochSecond();
            String nonceStr = cn.hutool.core.util.StrUtil.uuid().replace("-", "");

            String signStr = config.appId + "\n" + timestamp + "\n" + nonceStr + "\n" + prepayId + "\n";
            String sign = signWithMchKey(signStr, config.mchKey);

            Map<String, String> invokeParams = new LinkedHashMap<>();
            invokeParams.put("appid", config.appId);
            invokeParams.put("partnerid", config.mchId);
            invokeParams.put("prepayid", prepayId);
            invokeParams.put("package", "Sign=WXPay");
            invokeParams.put("noncestr", nonceStr);
            invokeParams.put("timestamp", String.valueOf(timestamp));
            invokeParams.put("sign", sign);

            return invokeParams;

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信App支付异常: orderId={}", orderId, e);
            throw new BizException(6005, "微信App支付异常: " + e.getMessage());
        }
    }

    private String executeV3Post(WxPayAccountConfig config, String path, String requestBody) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        String nonceStr = cn.hutool.core.util.StrUtil.uuid().replace("-", "");
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
            log.error("微信API请求失败: path={}, status={}, body={}", path, statusCode, body);
            JSONObject errResp = JSONUtil.parseObj(body);
            String errMsg = errResp.getStr("message", "请求失败");
            throw new BizException(6005, "微信App支付API错误: " + errMsg);
        }

        return body;
    }

    /**
     * 构建签名串（V3 签名算法）。
     */
    private String buildSignature(WxPayAccountConfig config, String method,
                                   String path, long timestamp, String nonce, String body) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(config.getPrivateKeyObj());
            String signStr = method + "\n" + path + "\n" + timestamp + "\n" + nonce + "\n" + body + "\n";
            sig.update(signStr.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
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

    /**
     * App 调起签名：使用 APIv3 密钥对调起参数进行 HMAC-SHA256 签名。
     */
    private String signWithMchKey(String data, String mchKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(mchKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] signBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signBytes);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256签名失败", e);
        }
    }
}
