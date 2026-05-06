package com.payflow.cashier.service;

import com.payflow.cashier.dto.*;

/**
 * 订单服务接口
 *
 * @author PayFlow Team
 */
public interface OrderService {

    CreateOrderResponse createOrder(CreateOrderRequest request);

    OrderDetailResponse getOrderDetail(String orderId);

    /**
     * @param clientType 终端类型：PC / H5 / APP（可空；为空时不按终端过滤）
     */
    CashierResponse getCashierInfo(String orderId, String clientType);

    void updateOrderStatus(String orderId, String newStatus, Long payAmount);

    /**
     * 商户查询接口（优先读缓存，未命中查DB）
     * 校验 merchantId 匹配，订单不存在返回 null
     *
     * @param orderId    平台订单号
     * @param merchantId 商户号
     * @return 订单详情，不存在或 merchantId 不匹配返回 null
     */
    OrderDetailResponse getOrderByMerchant(String orderId, String merchantId);
}
