package com.payflow.admin.service;

/**
 * 将管理端商户支付配置同步至收银台实付路由表。
 */
public interface MerchantCashierRouteSyncService {

    /**
     * 按商户号重建 cashier_channel_merchant_routes，并发布收银台配置刷新事件（若 Redis 已启用）。
     */
    void syncAndNotify(String merchantId);
}
