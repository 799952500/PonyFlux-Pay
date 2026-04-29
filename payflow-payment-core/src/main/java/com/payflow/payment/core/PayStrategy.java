package com.payflow.payment.core;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * 单笔支付方式的策略（由渠道子模块实现并注册）。
  * @author Lucas
 */
public interface PayStrategy {

    /**
     * @return 本策略对应的枚举，用于注册表路由
     */
    PayMethod getPayMethod();

    PayResult pay(String orderId, Long amount, String subject,
                  String returnUrl, String notifyUrl,
                  ChannelConfigHolder account,
                  Map<String, String> extraParams);

    RefundResult refund(String tradeNo, Long refundAmount,
                        String reason, ChannelConfigHolder account);

    NotifyResult parseNotify(HttpServletRequest request);
}
