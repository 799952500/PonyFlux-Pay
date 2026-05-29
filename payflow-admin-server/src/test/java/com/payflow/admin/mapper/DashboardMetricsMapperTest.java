package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.DashboardMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DashboardMetricsMapper 集成测试（需 MySQL，设置 PAYFLOW_INTEGRATION_TESTS=true 启用）。
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "PAYFLOW_INTEGRATION_TESTS", matches = "true")
@DisplayName("DashboardMetricsMapper 测试")
class DashboardMetricsMapperTest {

    @Autowired
    private DashboardMetricsMapper dashboardMetricsMapper;

    @Test
    @DisplayName("按时间范围和粒度查询")
    void queryByTimeRangeAndGranularity() {
        LocalDateTime start = LocalDateTime.now().minusDays(7);
        LocalDateTime end = LocalDateTime.now();

        List<DashboardMetrics> result = dashboardMetricsMapper.selectList(
                new LambdaQueryWrapper<DashboardMetrics>()
                        .ge(DashboardMetrics::getMetricTime, start)
                        .le(DashboardMetrics::getMetricTime, end)
                        .eq(DashboardMetrics::getGranularity, "daily")
                        .orderByDesc(DashboardMetrics::getMetricTime)
        );

        assertNotNull(result);
    }

    @Test
    @DisplayName("按渠道筛选查询")
    void queryByChannelCode() {
        List<DashboardMetrics> result = dashboardMetricsMapper.selectList(
                new LambdaQueryWrapper<DashboardMetrics>()
                        .eq(DashboardMetrics::getChannelCode, "wxpay")
                        .orderByDesc(DashboardMetrics::getMetricTime)
                        .last("LIMIT 10")
        );

        assertNotNull(result);
    }

    @Test
    @DisplayName("插入聚合指标")
    void insertMetrics() {
        DashboardMetrics metrics = new DashboardMetrics();
        metrics.setMetricTime(LocalDateTime.now());
        metrics.setGranularity("5min");
        metrics.setChannelCode("ALL");
        metrics.setTotalAmount(10000L);
        metrics.setTotalCount(5);
        metrics.setActiveMerchants(3);
        metrics.setFeeIncome(60L);
        metrics.setRefundAmount(0L);
        metrics.setRefundCount(0);

        int rows = dashboardMetricsMapper.insert(metrics);
        assertTrue(rows > 0);
        assertNotNull(metrics.getId());
        assertEquals(10000L, metrics.getTotalAmount());
    }
}
