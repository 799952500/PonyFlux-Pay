package com.payflow.payment.alipay;

import com.alipay.easysdk.factory.Factory;
import com.payflow.payment.core.ChannelConfigHolder;
import com.payflow.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 支付宝 WAP（手机网站支付）处理器。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
public class AliPayWapHandler {

    /**
     * 创建 WAP 网页支付订单。
     * 调用 alipay.trade.wap.pay 接口，生成用于 H5 页面自动提交的支付表单。
     */
    public AliPayOrderResponse createWapOrder(String orderId, Long amount, String subject,
                                              String returnUrl, String notifyUrl,
                                              ChannelConfigHolder account) {
        try {
            AliPayClientCache.configure(account);
            String totalAmount = String.valueOf(amount / 100.0);

            // Wap.pay(subject, outTradeNo, totalAmount, quitUrl, returnUrl) + asyncNotify(url)
            var response = Factory.Payment.Wap()
                    .asyncNotify(notifyUrl)
                    .pay(subject, orderId, totalAmount, "", returnUrl);

            // WapPayResponse 只有 body 字段（HTML 表单）
            // 成功时返回包含 <form> 的 HTML，失败时 body 为错误 JSON
            if (response.body != null && response.body.contains("<form")) {
                log.info("支付宝WAP支付下单成功: orderId={}", orderId);
                return AliPayOrderResponse.builder()
                        .formHtml(response.body)
                        .build();
            } else {
                String errMsg = extractErrorMsg(response.body);
                log.error("支付宝WAP支付下单失败: orderId={}, body={}", orderId, response.body);
                throw new BizException(6006, "支付宝WAP支付下单失败: " + errMsg);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("支付宝WAP支付异常: orderId={}", orderId, e);
            throw new BizException(6006, "支付宝WAP支付异常: " + e.getMessage());
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
