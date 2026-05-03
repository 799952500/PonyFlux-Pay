package com.payflow.admin.controller;

import com.payflow.admin.service.DashboardAggregationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据概览仪表盘 Controller
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardAggregationService dashboardAggregationService;

    /**
     * 数据概览首页（根端点）：一次返回 KPI、趋势、渠道占比、最新订单。
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> dashboard(
            @RequestParam(defaultValue = "7") int trendDays) {
        Map<String, Object> data = dashboardAggregationService.buildDashboardPayload(trendDays);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", data
        ));
    }

    /**
     * 获取今日核心统计指标（兼容旧客户端；数据源于聚合服务）。
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats(
            @RequestParam(defaultValue = "7") int trendDays) {
        Map<String, Object> full = dashboardAggregationService.buildDashboardPayload(trendDays);
        Map<String, Object> legacy = new LinkedHashMap<>();
        legacy.put("todayRevenue", full.get("todayRevenue"));
        legacy.put("todayOrders", full.get("todayOrders"));
        legacy.put("todayRefunds", full.get("todayRefunds"));
        legacy.put("activeMerchants", full.get("activeMerchants"));
        legacy.put("successRate", full.get("successRate"));
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", legacy
        ));
    }

    /**
     * 收入趋势数据
     */
    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> trend(
            @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> full = dashboardAggregationService.buildDashboardPayload(days);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trendData = (List<Map<String, Object>>) full.get("trendData");
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", trendData != null ? trendData : List.of()
        ));
    }

    /**
     * 各渠道交易分布
     */
    @GetMapping("/channel-dist")
    public ResponseEntity<Map<String, Object>> channelDist(
            @RequestParam(defaultValue = "7") int trendDays) {
        Map<String, Object> full = dashboardAggregationService.buildDashboardPayload(trendDays);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> channelDistribution =
                (List<Map<String, Object>>) full.get("channelDistribution");
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", channelDistribution != null ? channelDistribution : List.of()
        ));
    }
}
