package com.payflow.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of(
                        "todayRevenue", new BigDecimal("128450.00"),
                        "todayOrders", 3847,
                        "todayRefunds", 23,
                        "activeMerchants", 156,
                        "successRate", "99.2%"
                )
        ));
    }

    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> trend(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                List.of(
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

    @GetMapping("/channel-dist")
    public ResponseEntity<Map<String, Object>> channelDist() {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                List.of(
                        Map.of("channel", "Alipay", "count", 4521, "amount", 589200, "ratio", "38.5%"),
                        Map.of("channel", "WeChat Pay", "count", 3890, "amount", 476300, "ratio", "32.2%"),
                        Map.of("channel", "UnionPay", "count", 2340, "amount", 298700, "ratio", "20.1%"),
                        Map.of("channel", "Credit Card", "count", 1100, "amount", 189000, "ratio", "9.2%")
                )
        ));
    }
}
