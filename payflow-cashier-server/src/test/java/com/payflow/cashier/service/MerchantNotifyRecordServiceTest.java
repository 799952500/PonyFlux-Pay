package com.payflow.cashier.service;

import com.payflow.cashier.dto.MqMessage;
import com.payflow.cashier.entity.MerchantNotify;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.mapper.MerchantNotifyAttemptMapper;
import com.payflow.cashier.mapper.MerchantNotifyMapper;
import com.payflow.cashier.service.impl.MerchantNotifyRecordServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MerchantNotifyRecordService 写库测试")
class MerchantNotifyRecordServiceTest {

    @Mock
    private MerchantNotifyMapper merchantNotifyMapper;
    @Mock
    private MerchantNotifyAttemptMapper merchantNotifyAttemptMapper;

    @InjectMocks
    private MerchantNotifyRecordServiceImpl recordService;

    @Test
    @DisplayName("未配置回调地址写入 NOT_CONFIGURED")
    void recordNotConfiguredStatus() {
        Order order = Order.builder()
                .orderId("ORD-1")
                .merchantId("M100001")
                .merchantOrderNo("MO-1")
                .status("PAID")
                .build();
        MqMessage message = MqMessage.of(order.getOrderId(), "PAID", "PAY-1");

        when(merchantNotifyMapper.selectOne(any())).thenReturn(null);

        recordService.recordNotConfigured(order, message);

        ArgumentCaptor<MerchantNotify> captor = ArgumentCaptor.forClass(MerchantNotify.class);
        verify(merchantNotifyMapper).insert(captor.capture());
        assertEquals("NOT_CONFIGURED", captor.getValue().getSummaryStatus());
        assertEquals("PAYMENT", captor.getValue().getNotifyType());
        assertEquals(0, captor.getValue().getAttemptCount());
    }

    @Test
    @DisplayName("退款通知写入 REFUND 类型")
    void recordNotConfiguredForRefundType() {
        Order order = Order.builder()
                .orderId("ORD-2")
                .merchantId("M100001")
                .status("PAID")
                .build();
        MqMessage message = MqMessage.ofRefundMerchantNotify("ORD-2", "PAID", "PAY-1", "REF-1", 100L);
        when(merchantNotifyMapper.selectOne(any())).thenReturn(null);

        recordService.recordNotConfigured(order, message);

        ArgumentCaptor<MerchantNotify> captor = ArgumentCaptor.forClass(MerchantNotify.class);
        verify(merchantNotifyMapper).insert(captor.capture());
        assertEquals("REFUND", captor.getValue().getNotifyType());
    }
}
