package com.payflow.admin.service;

import com.payflow.admin.client.CashierInternalClient;
import com.payflow.admin.dto.AdminOrderRefundRequest;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.entity.cashier.Payment;
import com.payflow.admin.kit.AdminRequestContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 订单运维操作：待审批退款申请、支付机构查单同步。
 */
@Service
@RequiredArgsConstructor
public class AdminOrderOpsService {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final CashierInternalClient cashierInternalClient;

    public Map<String, Object> createRefundRequest(String orderId, AdminOrderRefundRequest body,
                                                    List<String> merchantScopeIds) {
        Order order = requireVisibleOrder(orderId, merchantScopeIds);
        Payment payment = requirePaymentOfOrder(orderId, body.getPaymentId());
        if (!order.getOrderId().equals(payment.getOrderId())) {
            throw new IllegalArgumentException("支付记录不属于该订单");
        }
        return cashierInternalClient.createPendingRefund(orderId, body);
    }

    public Map<String, Object> queryPaymentChannel(String orderId, String paymentId, boolean sync,
                                                    List<String> merchantScopeIds) {
        requireVisibleOrder(orderId, merchantScopeIds);
        requirePaymentOfOrder(orderId, paymentId);
        return cashierInternalClient.queryPaymentChannel(paymentId, sync);
    }

    private Order requireVisibleOrder(String orderId, List<String> merchantScopeIds) {
        Order order = orderService.getByOrderId(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        String merchantFilter = AdminRequestContext.resolveMerchantFilter(null, merchantScopeIds);
        if ("__NO_ACCESS__".equals(merchantFilter)) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (merchantFilter != null && !merchantFilter.equals(order.getMerchantId())) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (merchantScopeIds != null && !merchantScopeIds.isEmpty()
                && (order.getMerchantId() == null || !merchantScopeIds.contains(order.getMerchantId()))) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }

    private Payment requirePaymentOfOrder(String orderId, String paymentId) {
        List<Payment> payments = paymentService.findByOrderId(orderId);
        return payments.stream()
                .filter(p -> paymentId.equals(p.getPaymentId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("支付记录不存在"));
    }
}
