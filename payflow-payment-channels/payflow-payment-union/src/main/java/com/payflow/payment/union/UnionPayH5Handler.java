package com.payflow.payment.union;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 银联 H5 支付处理器：构建 H5 支付请求，调用银联网关获取重定向 URL。
 *
 * @author PayFlow Team
 */
@Slf4j
public class UnionPayH5Handler {

    /**
     * 构建 H5 支付重定向 URL。
     *
     * @param orderId    商户订单号
     * @param amount     支付金额（分）
     * @param subject    商品描述
     * @param returnUrl  支付完成前端跳转地址（frontUrl）
     * @param notifyUrl  异步通知地址（backUrl）
     * @param config     银联账号配置
     * @return 银联支付页面完整 URL
     */
    public String pay(String orderId, Long amount, String subject,
                      String returnUrl, String notifyUrl,
                      UnionPayAccountConfig config) {
        UnionPayHttpClient client = new UnionPayHttpClient(config);

        Map<String, String> bizParams = new HashMap<>();
        bizParams.put("orderId", orderId);
        bizParams.put("txnAmt", amount != null ? amount.toString() : "0");

        String h5Url = client.frontTransUrl(bizParams, returnUrl, notifyUrl);
        log.info("银联H5支付下单: orderId={}, amount={}, url={}", orderId, amount, h5Url);
        return h5Url;
    }
}
