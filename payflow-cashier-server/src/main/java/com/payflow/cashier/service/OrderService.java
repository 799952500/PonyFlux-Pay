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

    CashierResponse getCashierInfo(String orderId);

    void updateOrderStatus(String orderId, String newStatus, Long payAmount);
}
