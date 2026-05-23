package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.entity.cashier.MerchantNotify;
import com.payflow.admin.entity.cashier.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商户回调记录查询（cashier 库）。
 */
public interface MerchantNotifyQueryService {

    IPage<Map<String, Object>> page(int pageNum, int pageSize,
                                   String merchantId, String orderId, String merchantOrderNo,
                                   String notifyType, String summaryStatus,
                                   LocalDateTime startTime, LocalDateTime endTime,
                                   List<String> merchantScopeIds);

    Map<String, Object> getDetail(String notifyId, List<String> merchantScopeIds);

    Map<String, Object> getByOrder(String orderId, String notifyType, List<String> merchantScopeIds);

    MerchantNotify getSummaryEntity(String notifyId);

    Order getOrderIfAllowed(String orderId, List<String> merchantScopeIds);
}
