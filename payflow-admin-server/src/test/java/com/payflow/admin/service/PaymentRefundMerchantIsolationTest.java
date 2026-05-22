package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.client.CashierInternalRefundClient;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.entity.cashier.Payment;
import com.payflow.admin.entity.cashier.Refund;
import com.payflow.admin.mapper.cashier.OrderMapper;
import com.payflow.admin.mapper.cashier.PaymentMapper;
import com.payflow.admin.mapper.cashier.RefundMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("支付与退款商户隔离测试")
class PaymentRefundMerchantIsolationTest {

    @Mock
    private PaymentMapper paymentMapper;

    @Mock
    private RefundMapper refundMapper;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private CashierInternalRefundClient cashierInternalRefundClient;

    @Test
    @DisplayName("支付详情按支付单号查询后保留原始订单归属信息")
    void paymentDetailKeepsOrderReferenceForOwnershipCheck() {
        PaymentService service = new PaymentService(paymentMapper, orderMapper);
        Payment payment = new Payment();
        payment.setPaymentId("PAY-1");
        payment.setOrderId("ORD-1");
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        Payment result = service.getByPaymentId("PAY-1");

        assertEquals("ORD-1", result.getOrderId());
    }

    @Test
    @DisplayName("支付详情按订单归属拒绝授权外商户")
    void paymentDetailRejectsPaymentOutsideMerchantScope() {
        PaymentService service = new PaymentService(paymentMapper, orderMapper);
        Payment payment = new Payment();
        payment.setPaymentId("PAY-2");
        payment.setOrderId("ORD-2");
        when(paymentMapper.selectOne(any())).thenReturn(payment);
        when(orderMapper.selectOne(any())).thenReturn(order("ORD-2", "M100002"));

        Payment result = service.getByPaymentId("PAY-2", List.of("M100001"));

        assertEquals(null, result);
    }

    @Test
    @DisplayName("支付详情允许系统管理员跨商户查看")
    void paymentDetailAllowsSystemAdminScope() {
        PaymentService service = new PaymentService(paymentMapper, orderMapper);
        Payment payment = new Payment();
        payment.setPaymentId("PAY-3");
        payment.setOrderId("ORD-3");
        when(paymentMapper.selectOne(any())).thenReturn(payment);

        Payment result = service.getByPaymentId("PAY-3", null);

        assertEquals("ORD-3", result.getOrderId());
    }

    @Test
    @DisplayName("退款列表使用商户范围过滤")
    void refundPageAppliesMerchantScope() {
        AdminRefundService service = new AdminRefundService(refundMapper, orderMapper, cashierInternalRefundClient);
        Refund refund = refund("REF-1", "ORD-1", Refund.STATUS_REFUNDING);
        Order order = order("ORD-1", "M100001");
        Page<Refund> page = new Page<>();
        page.setRecords(List.of(refund));
        when(refundMapper.selectPage(any(), any())).thenReturn(page);
        when(orderMapper.selectOne(any())).thenReturn(order);

        IPage<Map<String, Object>> result = service.page(1, 20, null, null, null, null, List.of("M100001"));

        assertEquals(1, result.getRecords().size());
        assertEquals("REF-1", result.getRecords().get(0).get("refundId"));
        verify(refundMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("空商户授权范围查询退款返回空结果")
    void refundPageWithEmptyScopeReturnsEmptyResult() {
        AdminRefundService service = new AdminRefundService(refundMapper, orderMapper, cashierInternalRefundClient);
        Page<Refund> page = new Page<>();
        when(refundMapper.selectPage(any(), any())).thenReturn(page);

        IPage<Map<String, Object>> result = service.page(1, 20, null, null, null, null, List.of());

        assertEquals(0, result.getRecords().size());
    }

    @Test
    @DisplayName("审批授权外退款时拒绝且不调用收银台")
    void approveRejectsRefundOutsideMerchantScope() {
        AdminRefundService service = new AdminRefundService(refundMapper, orderMapper, cashierInternalRefundClient);
        Refund refund = refund("REF-1", "ORD-2", Refund.STATUS_REFUNDING);
        when(refundMapper.selectOne(any())).thenReturn(refund);
        when(orderMapper.selectOne(any())).thenReturn(order("ORD-2", "M100002"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.approve("REF-1", List.of("M100001")));

        assertEquals("无权操作该退款", ex.getMessage());
        verify(cashierInternalRefundClient, never()).executeRefund(any());
    }

    @Test
    @DisplayName("拒绝授权外退款时不更新退款状态")
    void rejectDoesNotUpdateRefundOutsideMerchantScope() {
        AdminRefundService service = new AdminRefundService(refundMapper, orderMapper, cashierInternalRefundClient);
        Refund refund = refund("REF-1", "ORD-2", Refund.STATUS_REFUNDING);
        when(refundMapper.selectOne(any())).thenReturn(refund);
        when(orderMapper.selectOne(any())).thenReturn(order("ORD-2", "M100002"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> service.reject("REF-1", List.of("M100001")));

        assertEquals("无权操作该退款", ex.getMessage());
        verify(refundMapper, never()).updateById(org.mockito.ArgumentMatchers.<Refund>any());
    }

    private static Refund refund(String refundId, String orderId, String status) {
        Refund refund = new Refund();
        refund.setRefundId(refundId);
        refund.setOrderId(orderId);
        refund.setRefundAmount(100L);
        refund.setStatus(status);
        return refund;
    }

    private static Order order(String orderId, String merchantId) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setMerchantId(merchantId);
        order.setMerchantOrderNo("MO-" + orderId);
        return order;
    }
}
