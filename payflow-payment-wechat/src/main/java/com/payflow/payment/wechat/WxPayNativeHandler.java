package com.payflow.payment.wechat;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 微信支付 Native（扫码支付）处理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxPayNativeHandler {

    private final WxPayV3HttpClient wxPayV3HttpClient;

    /**
     * 创建 Native 扫码支付订单。
     */
    public WxPayNativeResponse createQrCodeOrder(String orderId, Long amount,
                                                 String description, String notifyUrl,
                                                 ChannelConfigHolder account) {
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

            String requestBody = body.toString();
            String responseBody = wxPayV3HttpClient.postJson(config,
                    "/v3/pay/transactions/native",
                    requestBody,
                    "Native");

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
     * 申请微信支付退款。
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

            JSONObject fundsAccount = new JSONObject();
            fundsAccount.set("account", "UNSET");
            body.set("funds_account", fundsAccount);

            JSONObject amountJson = new JSONObject();
            amountJson.set("refund", refundAmount);
            long total = totalAmount != null ? totalAmount : refundAmount;
            amountJson.set("total", total);
            amountJson.set("currency", "CNY");
            body.set("amount", amountJson);

            String requestBody = body.toString();
            String responseBody = wxPayV3HttpClient.postJson(config,
                    "/v3/refund/domestic/refunds",
                    requestBody,
                    "Refund");

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
            String body = wxPayV3HttpClient.get(config, path, "QueryOrder");
            JSONObject resp = JSONUtil.parseObj(body);
            return "SUCCESS".equals(resp.getStr("trade_state"));
        } catch (Exception e) {
            log.warn("微信查单失败: outTradeNo={}, error={}", outTradeNo, e.getMessage());
            return false;
        }
    }
}
