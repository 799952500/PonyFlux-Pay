package com.payflow.payment.alipay;

import com.alipay.easysdk.factory.Factory;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝 App 支付处理器。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class AliPayAppHandler {

    /**
     * 创建 App 支付订单。
     * 调用 alipay.trade.app.pay 接口，返回App调起参数字符串。
     */
    public String createAppOrder(String orderId, Long amount, String subject,
                                 String notifyUrl, ChannelConfigHolder account) {
        try {
            AliPayClientCache.configure(account);
            String totalAmount = String.valueOf(amount / 100.0);

            var response = Factory.Payment.App()
                    .asyncNotify(notifyUrl)
                    .pay(subject, orderId, totalAmount);

            // AppPayResponse 只有 body 字段（App 调起参数）
            // 成功时返回形如 "alipay_sdk=alipay-sdk-java&..."
            // 失败时 body 为错误 JSON
            if (response.body != null && response.body.contains("alipay_sdk")) {
                log.info("支付宝App支付下单成功: orderId={}", orderId);
                return response.body;
            } else {
                // body 为错误 JSON，返回 code/msg
                String errMsg = extractErrorMsg(response.body);
                log.error("支付宝App支付下单失败: orderId={}, body={}", orderId, response.body);
                throw new BizException(6006, "支付宝App支付下单失败: " + errMsg);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝App支付异常: orderId={}", orderId, e);
            throw new BizException(6006, "支付宝App支付异常: " + e.getMessage());
        }
    }

    private String extractErrorMsg(String body) {
        if (body == null) return "未知错误";
        if (body.contains("\"msg\"")) {
            int idx = body.indexOf("\"msg\"");
            int start = body.indexOf("\"", idx + 6);
            int end = body.indexOf("\"", start + 1);
            if (start > 0 && end > start) {
                return body.substring(start + 1, end);
            }
        }
        return body;
    }
}
