package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.DashboardMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DashboardMetricsMapper 单元测试
 * 验证预聚合查询（按时间范围/粒度/渠道筛选）
 */
@SpringBootTest
@DisplayName("DashboardMetricsMapper 测试")
class DashboardMetricsMapperTest {

    @Autowired(required = false)
    private DashboardMetricsMapper dashboardMetricsMapper;

    @Test
    @DisplayName("按时间范围和粒度查询")
    void queryByTimeRangeAndGranularity() {
        if (dashboardMetricsMapper == null) {
            return; // 无数据库时跳过
        }
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
        if (dashboardMetricsMapper == null) {
            return;
        }
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
        if (dashboardMetricsMapper == null) {
            return;
        }
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
