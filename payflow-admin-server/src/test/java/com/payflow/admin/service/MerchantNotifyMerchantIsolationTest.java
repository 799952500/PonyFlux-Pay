package com.payflow.admin.service;

import com.payflow.admin.entity.cashier.MerchantNotify;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.mapper.cashier.MerchantNotifyMapper;
import com.payflow.admin.service.NotificationService;
import com.payflow.admin.service.impl.MerchantNotifyQueryServiceImpl;
import com.payflow.common.exception.BizException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("商户回调查询隔离测试")
class MerchantNotifyMerchantIsolationTest {

    @Mock
    private MerchantNotifyMapper merchantNotifyMapper;
    @Mock
    private com.payflow.admin.mapper.cashier.MerchantNotifyAttemptMapper merchantNotifyAttemptMapper;
    @Mock
    private OrderService orderService;

    @Mock
    private NotificationService notificationService;

    @Test
    @DisplayName("跨商户订单查询应拒绝")
    void getByOrderRejectsCrossMerchant() {
        MerchantNotifyQueryService service = new MerchantNotifyQueryServiceImpl(
                merchantNotifyMapper, merchantNotifyAttemptMapper, orderService, notificationService);
        Order order = Order.builder().orderId("ORD-B").merchantId("M100002").build();
        when(orderService.getByOrderId("ORD-B")).thenReturn(order);

        assertThrows(BizException.class,
                () -> service.getByOrder("ORD-B", null, List.of("M100001")));
    }

    @Test
    @DisplayName("空授权范围列表查询返回空")
    void pageReturnsEmptyForEmptyScope() {
        MerchantNotifyQueryService service = new MerchantNotifyQueryServiceImpl(
                merchantNotifyMapper, merchantNotifyAttemptMapper, orderService, notificationService);

        var page = service.page(1, 20, null, null, null, null, null, null, null, List.of());

        assertEquals(0, page.getTotal());
        verify(merchantNotifyMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("跨商户 notifyId 详情应拒绝")
    void getDetailRejectsCrossMerchant() {
        MerchantNotifyQueryService service = new MerchantNotifyQueryServiceImpl(
                merchantNotifyMapper, merchantNotifyAttemptMapper, orderService, notificationService);
        when(merchantNotifyMapper.selectOne(any())).thenReturn(
                MerchantNotify.builder().notifyId("MN1").merchantId("M100002").orderId("ORD-B").build());

        assertThrows(BizException.class,
                () -> service.getDetail("MN1", List.of("M100001")));
    }
}
