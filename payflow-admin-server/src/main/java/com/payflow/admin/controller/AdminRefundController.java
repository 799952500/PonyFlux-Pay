package com.payflow.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listRefunds(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of(
                        "total", 23,
                        "page", page,
                        "size", size,
                        "list", List.of(
                                Map.of("refundId", "REF20260412001", "orderId", "ORD20260412001", "amount", new BigDecimal("299.00"), "reason", "用户取消", "status", "PENDING", "createdAt", "2026-04-12T12:00:00"),
                                Map.of("refundId", "REF20260412002", "orderId", "ORD20260412002", "amount", new BigDecimal("1280.50"), "reason", "重复扣款", "status", "APPROVED", "createdAt", "2026-04-12T13:30:00")
                        )
                )
        ));
    }
}
