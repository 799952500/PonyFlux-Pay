package com.payflow.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of(
                        "total", 3847,
                        "page", page,
                        "size", size,
                        "list", List.of(
                                Map.of("orderId", "ORD20260412001", "merchantId", "M001", "amount", new BigDecimal("299.00"), "channel", "Alipay", "status", "SUCCESS", "createdAt", "2026-04-12T10:23:45"),
                                Map.of("orderId", "ORD20260412002", "merchantId", "M002", "amount", new BigDecimal("1280.50"), "channel", "WeChat Pay", "status", "SUCCESS", "createdAt", "2026-04-12T10:35:12"),
                                Map.of("orderId", "ORD20260412003", "merchantId", "M003", "amount", new BigDecimal("599.00"), "channel", "UnionPay", "status", "PENDING", "createdAt", "2026-04-12T11:02:33")
                        )
                )
        ));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of(
                        "orderId", orderId,
                        "merchantId", "M001",
                        "merchantName", "测试商户",
                        "amount", new BigDecimal("299.00"),
                        "channel", "Alipay",
                        "status", "SUCCESS",
                        "createdAt", "2026-04-12T10:23:45",
                        "paidAt", "2026-04-12T10:24:01"
                )
        ));
    }

    @PostMapping("/{orderId}/close")
    public ResponseEntity<Map<String, Object>> closeOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of("orderId", orderId, "status", "CLOSED")
        ));
    }
}
