package com.payflow.admin.service;

import com.payflow.admin.entity.ChannelRoute;

import java.util.List;
import java.util.Map;
/**
 * @author Lucas
 */

public interface ChannelRouteService {
    List<Map<String, Object>> listWithDetails();

    List<Map<String, String>> listAllMerchantsSimple();

    ChannelRoute create(ChannelRoute route);

    /**
     * 为商户绑定渠道收款账号（已存在则跳过）。
     */
    void ensureMerchantAccountLink(String merchantId, Long channelId, Long paymentAccountId);

    void toggle(Long id);

    void delete(Long id);
}