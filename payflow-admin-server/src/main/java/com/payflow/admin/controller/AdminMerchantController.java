package com.payflow.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/merchants")
@RequiredArgsConstructor
public class AdminMerchantController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listMerchants(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of(
                        "total", 156,
                        "page", page,
                        "size", size,
                        "list", List.of(
                                Map.of("merchantId", "M001", "merchantName", "测试商户001", "status", "ACTIVE", "todayRevenue", 45680.00, "createdAt", "2026-01-01"),
                                Map.of("merchantId", "M002", "merchantName", "测试商户002", "status", "ACTIVE", "todayRevenue", 23450.00, "createdAt", "2026-01-15"),
                                Map.of("merchantId", "M003", "merchantName", "测试商户003", "status", "SUSPENDED", "todayRevenue", 0.00, "createdAt", "2026-02-01")
                        )
                )
        ));
    }

    @GetMapping("/{merchantId}")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable String merchantId) {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of(
                        "merchantId", merchantId,
                        "merchantName", "测试商户001",
                        "status", "ACTIVE",
                        "todayRevenue", 45680.00,
                        "todayOrders", 234,
                        "createdAt", "2026-01-01"
                )
        ));
    }
}
