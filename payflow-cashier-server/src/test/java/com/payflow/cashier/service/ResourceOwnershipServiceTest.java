package com.payflow.cashier.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.constant.MerchantSecurityErrorCodes;
import com.payflow.cashier.context.AuthMode;
import com.payflow.cashier.context.MerchantContext;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentLinkEntityMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.mapper.RefundMapper;
import com.payflow.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 资源所有权校验单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResourceOwnershipService 测试")
class ResourceOwnershipServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private RefundMapper refundMapper;
    @Mock
    private PaymentLinkEntityMapper paymentLinkEntityMapper;
    @Mock
    private SecurityAuditService securityAuditService;

    @InjectMocks
    private ResourceOwnershipService resourceOwnershipService;

    @AfterEach
    void tearDown() {
        MerchantContext.clear();
    }

    @Test
    @DisplayName("订单归属当前商户时通过")
    void allowsWhenOrderOwned() {
        MerchantContext.set("M001", AuthMode.JWT, "/api/v1/orders/O1", "127.0.0.1");
        Order order = new Order();
        order.setOrderId("O1");
        order.setMerchantId("M001");
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertDoesNotThrow(() -> resourceOwnershipService.assertOrderOwned(
                "O1", "GET", "/api/v1/orders/O1", "127.0.0.1", "ua"));
    }

    @Test
    @DisplayName("订单不存在时返回 5102")
    void deniesWhenOrderMissing() {
        MerchantContext.set("M001", AuthMode.JWT, "/api/v1/orders/O9", "127.0.0.1");
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BizException ex = assertThrows(BizException.class, () -> resourceOwnershipService.assertOrderOwned(
                "O9", "GET", "/api/v1/orders/O9", "127.0.0.1", "ua"));
        assertEquals(MerchantSecurityErrorCodes.RESOURCE_NOT_FOUND, ex.getCode());
        verify(securityAuditService).recordDenied(any(), isNull(), any(), any(), any(), any(), any(), any(), any(),
                eq(MerchantSecurityErrorCodes.RESOURCE_FORBIDDEN_INTERNAL), anyString());
    }

    @Test
    @DisplayName("订单属于其他商户时返回 5102")
    void deniesWhenOrderBelongsToOtherMerchant() {
        MerchantContext.set("M001", AuthMode.JWT, "/api/v1/orders/O2", "127.0.0.1");
        Order order = new Order();
        order.setOrderId("O2");
        order.setMerchantId("M002");
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        BizException ex = assertThrows(BizException.class, () -> resourceOwnershipService.assertOrderOwned(
                "O2", "GET", "/api/v1/orders/O2", "127.0.0.1", "ua"));
        assertEquals(MerchantSecurityErrorCodes.RESOURCE_NOT_FOUND, ex.getCode());
    }

    @Test
    @DisplayName("支付记录通过订单解析归属")
    void resolvesPaymentViaOrder() {
        MerchantContext.set("M001", AuthMode.HMAC, "/api/v1/refunds", "127.0.0.1");
        Payment payment = Payment.builder().paymentId("P1").orderId("O1").build();
        Order order = new Order();
        order.setOrderId("O1");
        order.setMerchantId("M001");
        when(paymentMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(payment);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(order);

        assertDoesNotThrow(() -> resourceOwnershipService.assertPaymentOwned(
                "P1", "POST", "/api/v1/refunds", "127.0.0.1", "ua"));
    }
}
