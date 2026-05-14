package com.payflow.admin.service;

import com.payflow.admin.entity.DashboardMetrics;
import com.payflow.admin.mapper.DashboardMetricsMapper;
import com.payflow.admin.mapper.cashier.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DashboardAggregationService 单元测试
 * 验证预聚合逻辑（从 cashier 表聚合写入 admin_dashboard_metrics）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardAggregationService 测试")
class DashboardAggregationServiceTest {

    @Mock
    private OrderMapper orderMapper;
    @Mock
    private DashboardMetricsMapper dashboardMetricsMapper;

    private DashboardAggregationService service;

    @BeforeEach
    void setUp() {
        service = new DashboardAggregationService(orderMapper, dashboardMetricsMapper);
    }

    @Test
    @DisplayName("按5分钟粒度聚合")
    void aggregate5min() {
        LocalDateTime start = LocalDateTime.now().minusMinutes(5);
        LocalDateTime end = LocalDateTime.now();

        when(orderMapper.aggregatePayments(any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
                .thenReturn(createMockPaymentStats());
        when(orderMapper.aggregateRefunds(any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
                .thenReturn(Collections.emptyList());
        when(dashboardMetricsMapper.insert(any(DashboardMetrics.class)))
                .thenReturn(1);

        int count = service.aggregateMetrics("5min", start, end);

        assertEquals(1, count);
        ArgumentCaptor<DashboardMetrics> captor = ArgumentCaptor.forClass(DashboardMetrics.class);
        verify(dashboardMetricsMapper, atLeastOnce()).insert(captor.capture());
        assertEquals("5min", captor.getValue().getGranularity());
        assertTrue(captor.getValue().getTotalAmount() >= 0);
    }

    @Test
    @DisplayName("按小时粒度聚合")
    void aggregateHourly() {
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now();

        when(orderMapper.aggregatePayments(any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
                .thenReturn(createMockPaymentStats());
        when(orderMapper.aggregateRefunds(any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
                .thenReturn(Collections.emptyList());
        when(dashboardMetricsMapper.insert(any(DashboardMetrics.class)))
                .thenReturn(1);

        int count = service.aggregateMetrics("hourly", start, end);

        assertTrue(count >= 0);
        verify(orderMapper).aggregatePayments(any(LocalDateTime.class), any(LocalDateTime.class), anyString());
    }

    @Test
    @DisplayName("无交易数据时聚合返回0")
    void aggregateEmptyData() {
        when(orderMapper.aggregatePayments(any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
                .thenReturn(Collections.emptyList());
        when(orderMapper.aggregateRefunds(any(LocalDateTime.class), any(LocalDateTime.class), anyString()))
                .thenReturn(Collections.emptyList());

        int count = service.aggregateMetrics("5min",
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now());

        assertEquals(0, count);
        verify(dashboardMetricsMapper, never()).insert(any(DashboardMetrics.class));
    }

    @Test
    @DisplayName("查询预聚合指标")
    void queryMetrics() {
        when(dashboardMetricsMapper.selectList(any()))
                .thenReturn(List.of(createMockMetrics()));

        Map<String, Object> result = service.queryMetrics(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now(),
                "daily",
                "ALL"
        );

        assertNotNull(result);
    }

    @Test
    @DisplayName("获取商户排行榜")
    void getMerchantRanking() {
        when(orderMapper.merchantRanking(any(LocalDateTime.class), any(LocalDateTime.class), anyInt()))
                .thenReturn(List.of(Map.of(
                        "merchantId", 1L,
                        "merchantName", "测试商户",
                        "totalAmount", 100000L,
                        "totalCount", 100L
                )));

        List<Map<String, Object>> ranking = service.getMerchantRanking(
                LocalDateTime.now().minusDays(30), LocalDateTime.now(), 10);

        assertNotNull(ranking);
        assertFalse(ranking.isEmpty());
        assertEquals(1L, ranking.get(0).get("merchantId"));
    }

    private List<Map<String, Object>> createMockPaymentStats() {
        return List.of(Map.of(
                "channelCode", "ALL",
                "totalAmount", 50000L,
                "totalCount", 10L,
                "activeMerchants", 5L,
                "feeIncome", 300L
        ));
    }

    private DashboardMetrics createMockMetrics() {
        DashboardMetrics m = new DashboardMetrics();
        m.setMetricTime(LocalDateTime.now());
        m.setGranularity("daily");
        m.setChannelCode("ALL");
        m.setTotalAmount(50000L);
        m.setTotalCount(10);
        m.setActiveMerchants(5);
        m.setFeeIncome(300L);
        m.setRefundAmount(0L);
        m.setRefundCount(0);
        return m;
    }
}
