package com.payflow.cashier.service;

import com.payflow.cashier.entity.WebhookDeliveryLog;

import java.util.List;

/**
 * Webhook 投递服务——通过 HTTP POST 将事件通知发送到商户配置的端点。
 *
 * @author PayFlow Team
 */
public interface WebhookDeliveryService {

    /**
     * 向单个端点投递消息（同步，由 @Async 包装）。
     *
     * @param deliveryLog 投递日志记录
     */
    void deliver(WebhookDeliveryLog deliveryLog);

    /**
     * 重试失败的投递。
     *
     * @param deliveryLog 待重试的投递记录
     * @return 重试是否成功
     */
    boolean retry(WebhookDeliveryLog deliveryLog);

    /**
     * 按订单 ID 查询投递日志。
     *
     * @param eventCode 事件代码
     * @param merchantId 商户号
     * @return 投递日志列表
     */
    List<WebhookDeliveryLog> findByEvent(String eventCode, String merchantId);
}
