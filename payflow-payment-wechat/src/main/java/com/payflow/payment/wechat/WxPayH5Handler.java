package com.payflow.payment.wechat;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * 微信支付 H5 支付处理器。
 * <p>
 * 调用微信支付 v3 API /v3/pay/transactions/h5 接口，
 * 返回 h5_url（拉起微信支付中间页的 URL）。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class WxPayH5Handler {

    /** 微信支付 API v3 base URL */
    private static final String API_BASE = "https://api.mch.weixin.qq.com";

    private final HttpClient httpClient;

    public WxPayH5Handler() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 创建 H5 支付订单。
     *
     * @param orderId     商户订单号（用作 outTradeNo）
     * @param amount      订单金额（分）
     * @param description 商品描述
     * @param notifyUrl   异步通知地址
     * @param account     渠道账户配置
     * @return H5 支付 URL（mweb_url），拉起微信支付的中间页
     */
    public String createH5Order(String orderId, Long amount,
                                String description, String notifyUrl,
                                String returnUrl,
                                ChannelConfigHolder account) {
        WxPayAccountConfig config = WxPayConfigLoader.load(account);

        try {
            // 构建 H5 场景信息
            JSONObject sceneInfo = new JSONObject();
            sceneInfo.set("payer_client_ip", "127.0.0.1"); // 生产环境应取真实 IP
            JSONObject h5Info = new JSONObject();
            h5Info.set("type", "Wap");
            sceneInfo.set("h5_info", h5Info);

            JSONObject body = new JSONObject();
            body.set("mchid", config.mchId);
            body.set("out_trade_no", orderId);
            body.set("appid", config.appId);
            body.set("description", description);
            body.set("notify_url", notifyUrl);
            body.set("scene_info", sceneInfo);

            JSONObject amountJson = new JSONObject();
            amountJson.set("total", amount);
            amountJson.set("currency", "CNY");
            body.set("amount", amountJson);

            String requestBody = body.toString();
            String responseBody = executeV3Post(config, "/v3/pay/transactions/h5", requestBody);

            JSONObject resp = JSONUtil.parseObj(responseBody);
            String h5Url = resp.getStr("h5_url");

            if (h5Url == null || h5Url.isBlank()) {
                throw new BizException(6005, "微信H5支付下单失败：未获取到 h5_url");
            }

            if (returnUrl != null && !returnUrl.isBlank()) {
                String encoded = URLEncoder.encode(returnUrl, StandardCharsets.UTF_8);
                String sep = h5Url.contains("?") ? "&" : "?";
                h5Url = h5Url + sep + "redirect_url=" + encoded;
            }

            log.info("微信H5支付下单成功: orderId={}, h5Url={}", orderId, h5Url);
            return h5Url;

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信H5支付异常: orderId={}", orderId, e);
            throw new BizException(6005, "微信H5支付异常: " + e.getMessage());
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
        String respBody = response.body();

        if (statusCode != 200 && statusCode != 201) {
            log.error("微信API请求失败: path={}, status={}, body={}", path, statusCode, respBody);
            JSONObject errResp = JSONUtil.parseObj(respBody);
            String errMsg = errResp.getStr("message", "请求失败");
            throw new BizException(6005, "微信H5支付API错误: " + errMsg);
        }

        return respBody;
    }

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
}
