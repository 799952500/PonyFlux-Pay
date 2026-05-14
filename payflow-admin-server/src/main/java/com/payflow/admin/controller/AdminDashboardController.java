package com.payflow.admin.controller;

import com.payflow.admin.service.DashboardAggregationService;
import com.payflow.admin.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据概览仪表盘 Controller
 *
 * @author Lucas
 */
@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardAggregationService dashboardAggregationService;
    private final OrderService orderService;

    /**
     * 数据概览首页（根端点）：一次返回 KPI、趋势、渠道占比、商户排行、最新订单。
     */
    @Operation(summary = "数据概览首页")
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
    @Operation(summary = "获取今日核心统计指标")
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
        legacy.put("revenueChangePct", full.get("revenueChangePct"));
        legacy.put("revenueYoYPct", full.get("revenueYoYPct"));
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", legacy
        ));
    }

    /**
     * 收入趋势数据
     */
    @Operation(summary = "收入趋势数据")
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
    @Operation(summary = "各渠道交易分布")
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

    /**
     * 从预聚合表查询仪表盘指标（支持自定义日期范围和粒度）。
     */
    @Operation(summary = "查询预聚合指标")
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics(
            @RequestParam(defaultValue = "day") String granularity,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "ALL") String channelCode) {
        Map<String, Object> data = dashboardAggregationService.queryMetrics(dateFrom, dateTo, granularity, channelCode);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", data
        ));
    }

    /**
     * 商户交易额排行榜
     */
    @Operation(summary = "商户交易额排行榜")
    @GetMapping("/merchant-ranking")
    public ResponseEntity<Map<String, Object>> merchantRanking(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "10") int limit) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(days);
        List<Map<String, Object>> ranking = dashboardAggregationService.getMerchantRanking(start, end, limit);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", ranking
        ));
    }

    /**
     * 商户交易洞察（近30天趋势、渠道偏好、退款率、最后交易时间）
     */
    @Operation(summary = "商户交易洞察")
    @GetMapping("/merchant/{merchantId}/insight")
    public ResponseEntity<Map<String, Object>> merchantInsight(
            @PathVariable String merchantId) {
        Map<String, Object> data = orderService.getMerchantInsight(merchantId);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", data
        ));
    }
}
