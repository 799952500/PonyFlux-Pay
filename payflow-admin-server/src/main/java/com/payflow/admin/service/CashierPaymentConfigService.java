package com.payflow.admin.service;

import java.util.List;
import java.util.Map;

/**
 * 收银台侧读取商户可用支付方式（基于商户支付路由）。
 *
 * @author Lucas
 */
public interface CashierPaymentConfigService {

    /**
     * @param merchantId       商户号
     * @param orderChannelCode 订单渠道编码（与收银台 Order.channel 一致，如 WECHAT_PAY）
     * @return 每项含 methodCode、methodName、description、priority、clientScopes（字符串列表）
     */
    List<Map<String, Object>> listPaymentMethodsForCashier(String merchantId, String orderChannelCode);
}
