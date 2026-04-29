package com.payflow.admin.controller;

import com.payflow.admin.kit.SystemConfigKit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据概览仪表盘 Controller
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    /**
     * 数据概览首页（根端点）
     *
     * @return 统计数据
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return stats();
    }

    /**
     * 获取今日核心统计指标
     *
     * @return 包含今日收入、订单数、退款数、商户数、成功率的数据
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        BigDecimal revenue = SystemConfigKit.getDecimal("dashboard_today_revenue", new BigDecimal("128450.00"));
        int orders = SystemConfigKit.getInt("dashboard_today_orders", 3847);
        int refunds = SystemConfigKit.getInt("dashboard_today_refunds", 23);
        int merchants = SystemConfigKit.getInt("dashboard_active_merchants", 156);
        String rate = SystemConfigKit.get("dashboard_success_rate", "99.2%");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayRevenue", revenue);
        data.put("todayOrders", orders);
        data.put("todayRefunds", refunds);
        data.put("activeMerchants", merchants);
        data.put("successRate", rate);

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", data
        ));
    }

    /**
     * 收入趋势数据
     *
     * @param days 统计天数（默认7天）
     * @return 每日收入与订单数列表
     */
    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> trend(
            @RequestParam(defaultValue = "7") int days) {
        // TODO: 接入真实数据库查询，替换为动态SQL
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", List.of(
                        Map.of("date", "2026-04-06", "revenue", 115200, "orders", 3420),
                        Map.of("date", "2026-04-07", "revenue", 98200, "orders", 2890),
                        Map.of("date", "2026-04-08", "revenue", 134500, "orders", 4100),
                        Map.of("date", "2026-04-09", "revenue", 121300, "orders", 3650),
                        Map.of("date", "2026-04-10", "revenue", 108900, "orders", 3200),
                        Map.of("date", "2026-04-11", "revenue", 145600, "orders", 4380),
                        Map.of("date", "2026-04-12", "revenue", 128450, "orders", 3847)
                )
        ));
    }

    /**
     * 各渠道交易分布
     *
     * @return 各渠道笔数、金额、占比
     */
    @GetMapping("/channel-dist")
    public ResponseEntity<Map<String, Object>> channelDist() {
        // TODO: 接入真实数据库查询，替换为动态SQL
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", List.of(
                        Map.of("channel", "Alipay", "count", 4521, "amount", 589200, "ratio", "38.5%"),
                        Map.of("channel", "WeChat Pay", "count", 3890, "amount", 476300, "ratio", "32.2%"),
                        Map.of("channel", "UnionPay", "count", 2340, "amount", 298700, "ratio", "20.1%"),
                        Map.of("channel", "Credit Card", "count", 1100, "amount", 189000, "ratio", "9.2%")
                )
        ));
    }
}
