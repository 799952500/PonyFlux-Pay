package com.payflow.payment.wechat;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.ChannelConfigHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 微信付款码支付（v3 /v3/pay/transactions/codepay）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxPayMicropayHandler {

    private final WxPayV3HttpClient wxPayV3HttpClient;

    /**
     * @return 解析结果：tradeState、transactionId（成功时）
     */
    public WxMicropayResult codePay(String orderId, Long amount, String description,
                                    String notifyUrl, ChannelConfigHolder account,
                                    String authCode) {
        if (authCode == null || authCode.isBlank()) {
            throw new BizException(6007, "付款码支付缺少 authCode（请传 CreatePaymentRequest.authCode）");
        }
        WxPayAccountConfig config = WxPayConfigLoader.load(account);
        try {
            JSONObject body = new JSONObject();
            body.set("mchid", config.mchId);
            body.set("out_trade_no", orderId);
            body.set("appid", config.appId);
            body.set("description", description);
            body.set("notify_url", notifyUrl);
            body.set("auth_code", authCode.trim());

            JSONObject amountJson = new JSONObject();
            amountJson.set("total", amount);
            amountJson.set("currency", "CNY");
            body.set("amount", amountJson);

            String responseBody = wxPayV3HttpClient.postJson(config,
                    "/v3/pay/transactions/codepay",
                    body.toString(),
                    "CodePay");

            JSONObject resp = JSONUtil.parseObj(responseBody);
            String tradeState = resp.getStr("trade_state");
            String transactionId = resp.getStr("transaction_id");
            log.info("微信付款码下单返回: orderId={}, trade_state={}", orderId, tradeState);
            return new WxMicropayResult(tradeState, transactionId);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("微信付款码支付异常: orderId={}", orderId, e);
            throw new BizException(6005, "微信付款码支付异常", e);
        }
    }

    /**
     * 付款码支付同步结果。
     */
    public record WxMicropayResult(String tradeState, String transactionId) {

        public boolean immediateSuccess() {
            return "SUCCESS".equals(tradeState) || "ACCEPTED".equals(tradeState);
        }
    }
}
