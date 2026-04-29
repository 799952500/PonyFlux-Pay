package com.payflow.payment.wechat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

/**
 * 微信支付 Native（扫码支付）处理器。
 * <p>
 * 调用微信支付 v3 API /v3/pay/transactions/native 接口，
 * 返回二维码链接 code_url，商户生成付款二维码供用户扫码支付。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class WxPayNativeHandler {

    /** 微信支付 API v3 base URL */
    private static final String API_BASE = "https://api.mch.weixin.qq.com";

    private final HttpClient httpClient;

    public WxPayNativeHandler() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    /**
     * 创建 Native 扫码支付订单。
     *
     * @param orderId    商户订单号（用作 outTradeNo）
     * @param amount     订单金额（分）
     * @param description 商品描述
     * @param notifyUrl  异步通知地址
     * @param account    渠道账户配置
     * @return 包含二维码链接的响应对象
     */
    public WxPayNativeResponse createQrCodeOrder(String orderId, Long amount,
                                                  String description, String notifyUrl,
                                                  ChannelConfigHolder account) {
        WxPayAccountConfig config = WxPayConfigLoader.load(account);

        try {
            // 构建请求 JSON
            JSONObject body = new JSONObject();
            body.set("mchid", config.mchId);
            body.set("out_trade_no", orderId);
            body.set("appid", config.appId);
            body.set("description", description);
            body.set("notify_url", notifyUrl);

            // 金额（分）
            JSONObject amountJson = new JSONObject();
            amountJson.set("total", amount);
            amountJson.set("currency", "CNY");
            body.set("amount", amountJson);

            String requestBody = body.toString();

            // 发送请求
            String responseBody = executeV3Post(config,
                    "/v3/pay/transactions/native",
                    requestBody);

            JSONObject resp = JSONUtil.parseObj(responseBody);
            String codeUrl = resp.getStr("code_url");
            String prepayId = resp.getStr("prepay_id");

            log.info("微信Native扫码支付下单成功: orderId={}, prepayId={}", orderId, prepayId);

            return WxPayNativeResponse.builder()
                    .tradeType("NATIVE")
                    .prepayId(prepayId)
                    .codeUrl(codeUrl)
                    .outTradeNo(orderId)
                    .build();

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信Native扫码支付异常: orderId={}", orderId, e);
            throw new BizException(6005, "微信Native扫码支付异常: " + e.getMessage());
        }
    }

    /**
     * 执行微信支付 v3 API 请求（自动签名）。
     */
    private String executeV3Post(WxPayAccountConfig config, String path,
                                   String requestBody) throws Exception {
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
            log.error("微信API请求失败: path={}, status={}, body={}", path, statusCode, body);
            JSONObject errResp = JSONUtil.parseObj(body);
            String errMsg = errResp.getStr("message", "请求失败");
            throw new BizException(6005, "微信Native支付API错误: " + errMsg);
        }

        return body;
    }

    /**
     * 构建签名串（V3 签名算法）。
     * <p>
     * 签名格式：HTTP_METHOD\n{path}\n{timestamp}\n{nonce_str}\n{body}\n
     * </p>
     */
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

    /**
     * 构建 Authorization 头。
     */
    private String buildAuthorization(WxPayAccountConfig config, long timestamp,
                                        String nonce, String signature) {
        return "WXAUTH appId=\"" + config.appId + "\", "
                + "nonceStr=\"" + nonce + "\", "
                + "timestamp=\"" + timestamp + "\", "
                + "signature=\"" + signature + "\"";
    }

    // ==================== 退款接口 ====================

    /**
     * 申请微信支付退款。
     * <p>
     * 调用微信支付 v3 API POST /v3/refund/domestic/refunds。
     * 需要商户证书（apiclient_cert.p12）进行双向认证。
     * </p>
     *
     * @param outTradeNo     原交易商户订单号
     * @param outRefundNo    商户侧退款单号
     * @param refundAmount   退款金额（分）
     * @param totalAmount    原订单总金额（分）
     * @param reason         退款原因
     * @param account        渠道账户配置
     * @return 渠道退款单号（refund_id）
     */
    public WxPayNativeResponse refund(String outTradeNo, String outRefundNo,
                                       Long refundAmount, Long totalAmount,
                                       String reason, ChannelConfigHolder account) {
        WxPayAccountConfig config = WxPayConfigLoader.load(account);

        try {
            JSONObject body = new JSONObject();
            body.set("out_refund_no", outRefundNo);
            body.set("out_trade_no", outTradeNo);
            body.set("reason", reason != null ? reason : "用户请求退款");

            // 退款资金去向
            JSONObject fundsAccount = new JSONObject();
            fundsAccount.set("account", "UNSET");
            body.set("funds_account", fundsAccount);

            // 金额信息
            JSONObject amountJson = new JSONObject();
            amountJson.set("refund", refundAmount);
            long total = totalAmount != null ? totalAmount : refundAmount;
            amountJson.set("total", total);
            amountJson.set("currency", "CNY");
            body.set("amount", amountJson);

            String requestBody = body.toString();
            String responseBody = executeV3Post(config, "/v3/refund/domestic/refunds", requestBody);

            JSONObject resp = JSONUtil.parseObj(responseBody);
            String refundId = resp.getStr("refund_id");
            String refundStatus = resp.getStr("status");

            log.info("微信退款申请成功: outTradeNo={}, refundId={}, status={}",
                    outTradeNo, refundId, refundStatus);

            return WxPayNativeResponse.builder()
                    .outTradeNo(outTradeNo)
                    .prepayId(refundId)
                    .build();

        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信退款异常: outTradeNo={}", outTradeNo, e);
            throw new BizException(6005, "微信退款异常: " + e.getMessage());
        }
    }

    /**
     * 按商户订单号查询微信支付状态是否为成功（关单前防误关）。
     */
    public boolean queryOutTradeNoSuccess(String outTradeNo, ChannelConfigHolder account) {
        WxPayAccountConfig config = WxPayConfigLoader.load(account);
        try {
            String path = "/v3/pay/transactions/out-trade-no/"
                    + URLEncoder.encode(outTradeNo, StandardCharsets.UTF_8)
                    + "?mchid=" + URLEncoder.encode(config.mchId, StandardCharsets.UTF_8);
            String body = executeV3Get(config, path);
            JSONObject resp = JSONUtil.parseObj(body);
            return "SUCCESS".equals(resp.getStr("trade_state"));
        } catch (Exception e) {
            log.warn("微信查单失败: outTradeNo={}, error={}", outTradeNo, e.getMessage());
            return false;
        }
    }

    private String executeV3Get(WxPayAccountConfig config, String pathWithQuery) throws Exception {
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
            log.error("微信API GET失败: path={}, status={}, body={}", pathWithQuery, statusCode, body);
            throw new BizException(6005, "微信查单API错误: HTTP " + statusCode);
        }
        return body;
    }
}
