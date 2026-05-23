package com.payflow.cashier.service;

import com.payflow.cashier.dto.MerchantNotifyDeliveryResult;
import com.payflow.cashier.dto.MqMessage;
import com.payflow.cashier.entity.MerchantNotify;
import com.payflow.cashier.entity.Order;

import java.util.Map;

/**
 * 商户回调记录持久化。
 */
public interface MerchantNotifyRecordService {

    /**
     * 未配置回调地址时记录汇总。
     */
    MerchantNotify recordNotConfigured(Order order, MqMessage message);

    /**
     * 开始一次 HTTP 尝试：递增序号并写入 IN_PROGRESS 明细。
     *
     * @return 汇总记录（含 notifyId）与本次 attemptNo
     */
    AttemptContext beginAttempt(Order order, MqMessage message, String notifyUrl,
                              Map<String, Object> requestParams, boolean signSkipped);

    /**
     * 完成一次尝试并更新汇总状态。
     */
    void finishAttempt(MerchantNotify summary, int attemptNo, Map<String, Object> requestParams,
                      MerchantNotifyDeliveryResult delivery, boolean willRetry);

    record AttemptContext(MerchantNotify summary, int attemptNo) {
    }
}
