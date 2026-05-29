package com.payflow.cashier.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.entity.Payment;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.mapper.PaymentMapper;
import com.payflow.cashier.metrics.PaymentMetrics;
import com.payflow.cashier.routing.ChannelHealthRedisService;
import com.payflow.cashier.webhook.WebhookDispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayNotifyServiceTest {

    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private OrderService orderService;
    @Mock
    private ChannelHealthRedisService channelHealthRedisService;
    @Mock
    private WebhookDispatchService webhookDispatchService;
    @Mock
    private PaymentMetrics paymentMetrics;
    @Mock
    private NotifyDedupService notifyDedupService;

    @InjectMocks
    private PayNotifyService payNotifyService;

    private Payment processingPayment;
    private Order order;

    @BeforeEach
    void setUp() {
        processingPayment = Payment.builder()
                .paymentId("PAY-001")
                .orderId("ORD-001")
                .status(Payment.STATUS_PROCESSING)
                .payChannel("wxpay")
                .amount(100L)
                .build();
        order = Order.builder()
                .orderId("ORD-001")
                .merchantId("M100001")
                .amount(100L)
                .currency("CNY")
                .build();
    }

    @Test
    void handlePaymentSuccess_skipsWhenAlreadySuccess() {
        processingPayment.setStatus(Payment.STATUS_SUCCESS);
        when(paymentMapper.selectOne(any())).thenReturn(processingPayment);

        payNotifyService.handlePaymentSuccess("ORD-001", "TX-1");

        verify(paymentMapper, never()).update(any(), any(UpdateWrapper.class));
        verify(webhookDispatchService, never()).publish(any(), any(), any());
    }

    @Test
    void handlePaymentSuccess_updatesAndPublishesOnce() {
        when(paymentMapper.selectOne(any())).thenReturn(processingPayment);
        when(paymentMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(notifyDedupService.tryMark(eq("PAY-001"), eq("PAYMENT_SUCCESS"))).thenReturn(true);

        payNotifyService.handlePaymentSuccess("ORD-001", "TX-1");

        verify(paymentMapper).update(any(), any(UpdateWrapper.class));
        verify(orderService).updateOrderStatus("ORD-001", Order.STATUS_PAID, 100L);
        verify(webhookDispatchService).publish(eq("M100001"), any(), any());
    }

    @Test
    void handlePaymentSuccess_skipsWebhookWhenDedupFails() {
        when(paymentMapper.selectOne(any())).thenReturn(processingPayment);
        when(paymentMapper.update(any(), any(UpdateWrapper.class))).thenReturn(1);
        when(orderMapper.selectOne(any())).thenReturn(order);
        when(notifyDedupService.tryMark(eq("PAY-001"), eq("PAYMENT_SUCCESS"))).thenReturn(false);

        payNotifyService.handlePaymentSuccess("ORD-001", "TX-1");

        verify(webhookDispatchService, never()).publish(any(), any(), any());
    }
}
