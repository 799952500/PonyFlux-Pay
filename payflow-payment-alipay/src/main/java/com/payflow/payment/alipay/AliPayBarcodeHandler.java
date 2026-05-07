package com.payflow.payment.alipay;

import com.alipay.easysdk.factory.Factory;
import com.payflow.common.exception.BizException;
import com.payflow.payment.core.ChannelConfigHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝条码/刷脸付款（被扫）：alipay.trade.pay。
 */
@Slf4j
@Component
public class AliPayBarcodeHandler {

    /**
     * 条码支付（用户付款码）。
     *
     * @param authCode 用户付款码
     * @return 是否同步支付成功、支付宝交易号
     */
    public AliPayBarcodePayResult pay(String orderId, Long amount, String subject,
                                      String notifyUrl, String authCode,
                                      ChannelConfigHolder account) {
        if (authCode == null || authCode.isBlank()) {
            throw new BizException(6007, "条码支付缺少 authCode（请传 CreatePaymentRequest.authCode）");
        }
        try {
            AliPayClientCache.configure(account);
            String totalAmount = String.valueOf(amount / 100.0);
            var response = Factory.Payment.FaceToFace()
                    .asyncNotify(notifyUrl)
                    .pay(subject, orderId, totalAmount, authCode.trim());

            if (response.code != null && "10000".equals(response.code)) {
                log.info("支付宝条码支付成功: orderId={}, tradeNo={}", orderId, response.tradeNo);
                return new AliPayBarcodePayResult(true, response.tradeNo, response.code);
            }
            String errMsg = response.msg != null ? response.msg : response.httpBody;
            log.warn("支付宝条码支付未成功: orderId={}, code={}, msg={}", orderId, response.code, errMsg);
            return new AliPayBarcodePayResult(false, response.tradeNo, response.code);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝条码支付异常: orderId={}", orderId, e);
            throw new BizException(6006, "支付宝条码支付异常: " + e.getMessage());
        }
    }

    /**
     * 条码支付结果摘要。
     */
    public record AliPayBarcodePayResult(boolean success, String tradeNo, String responseCode) {
    }
}
