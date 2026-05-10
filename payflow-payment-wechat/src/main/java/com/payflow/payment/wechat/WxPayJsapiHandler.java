package com.payflow.payment.wechat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.ChannelConfigHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Signature;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信 JSAPI / 小程序统一下单（v3 /v3/pay/transactions/jsapi）及调起参数签名。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxPayJsapiHandler {

    private final WxPayV3HttpClient wxPayV3HttpClient;

    /**
     * 统一下单，需用户 openid（公众号或小程序内支付）。
     */
    public String createJsapiPrepayId(String orderId, Long amount, String description,
                                      String notifyUrl, ChannelConfigHolder account,
                                      String payerOpenId) {
        if (payerOpenId == null || payerOpenId.isBlank()) {
            throw new BizException(6007, "JSAPI/小程序支付缺少 payerOpenId（请传 CreatePaymentRequest.openId）");
        }
        WxPayAccountConfig config = WxPayConfigLoader.load(account);
        try {
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

            JSONObject payer = new JSONObject();
            payer.set("openid", payerOpenId);
            body.set("payer", payer);

            String requestBody = body.toString();
            String responseBody = wxPayV3HttpClient.postJson(config,
                    "/v3/pay/transactions/jsapi",
                    requestBody,
                    "JSAPI");

            JSONObject resp = JSONUtil.parseObj(responseBody);
            String prepayId = resp.getStr("prepay_id");
            if (prepayId == null || prepayId.isBlank()) {
                throw new BizException(6005, "微信JSAPI下单失败：未返回 prepay_id");
            }
            log.info("微信JSAPI下单成功: orderId={}, prepayId={}", orderId, prepayId);
            return prepayId;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信JSAPI下单异常: orderId={}", orderId, e);
            throw new BizException(6005, "微信JSAPI下单异常", e);
        }
    }

    /**
     * 构建 H5/小程序调起微信支付所需参数（signType=RSA，paySign 为商户私钥签名）。
     */
    public Map<String, String> buildJsapiInvokeParams(WxPayAccountConfig config, String prepayId) {
        try {
            long timeStamp = Instant.now().getEpochSecond();
            String nonceStr = StrUtil.uuid().replace("-", "");
            String pkg = "prepay_id=" + prepayId;
            String message = config.appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + pkg + "\n";

            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(config.getPrivateKeyObj());
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            String paySign = java.util.Base64.getEncoder().encodeToString(sig.sign());

            Map<String, String> m = new LinkedHashMap<>();
            m.put("appId", config.appId);
            m.put("timeStamp", String.valueOf(timeStamp));
            m.put("nonceStr", nonceStr);
            m.put("package", pkg);
            m.put("signType", "RSA");
            m.put("paySign", paySign);
            return m;
        } catch (Exception e) {
            throw new BizException(6005, "构建微信JSAPI调起参数失败", e);
        }
    }
}
