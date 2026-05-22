package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.mapper.cashier.OrderMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 商户隔离测试")
class OrderServiceMerchantIsolationTest {

    @Mock
    private OrderMapper orderMapper;

    @Test
    @DisplayName("列表查询未传商户时限制在授权商户范围")
    void pageLimitsQueryToAuthorizedMerchantScope() {
        OrderService service = new OrderService(orderMapper);
        Page<Order> page = new Page<>();
        page.setRecords(List.of(order("ORD-1", "M100001")));
        when(orderMapper.selectPage(any(), any())).thenReturn(page);

        IPage<Order> result = service.page(1, 20, null, null, null, null, List.of("M100001"));

        assertEquals(1, result.getRecords().size());
        assertEquals("M100001", result.getRecords().get(0).getMerchantId());
        verify(orderMapper).selectPage(any(), any());
    }

    @Test
    @DisplayName("列表查询传入授权外商户时返回空页且不访问数据库")
    void pageReturnsEmptyWhenRequestedMerchantOutsideScope() {
        OrderService service = new OrderService(orderMapper);

        IPage<Order> result = service.page(1, 20, "M100002", null, null, null, List.of("M100001"));

        assertEquals(0, result.getTotal());
        assertEquals(List.of(), result.getRecords());
        verify(orderMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("空授权范围不可见任何订单")
    void emptyMerchantScopeReturnsEmptyPage() {
        OrderService service = new OrderService(orderMapper);

        IPage<Order> result = service.page(1, 20, null, null, null, null, List.of());

        assertEquals(0, result.getTotal());
        assertEquals(List.of(), result.getRecords());
        verify(orderMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("统计授权外商户订单数量返回 0")
    void countReturnsZeroForRequestedMerchantOutsideScope() {
        OrderService service = new OrderService(orderMapper);

        long result = service.count("M100002", List.of("M100001"));

        assertEquals(0L, result);
        verify(orderMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("导出授权外商户订单返回空列表且不访问数据库")
    void exportReturnsEmptyForRequestedMerchantOutsideScope() {
        OrderService service = new OrderService(orderMapper);

        List<Order> result = service.listForExport(100, "M100002", null, null, null, List.of("M100001"));

        assertEquals(List.of(), result);
        verify(orderMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("平台管理员未启用商户范围时保留请求商户筛选")
    void platformAdminKeepsRequestedMerchantFilter() {
        OrderService service = new OrderService(orderMapper);
        when(orderMapper.selectPage(any(), any())).thenReturn(new Page<>());

        service.page(1, 20, "M100002", null, null, null, null);

        ArgumentCaptor<Page<Order>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(orderMapper).selectPage(pageCaptor.capture(), any());
        assertEquals(20, pageCaptor.getValue().getSize());
    }

    private static Order order(String orderId, String merchantId) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setMerchantId(merchantId);
        return order;
    }
}
