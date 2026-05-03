package com.payflow.admin.controller;

import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.mapper.cashier.OrderMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局订单关键词检索（轻量 MVP）。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
public class AdminSearchController {

    private final OrderMapper orderMapper;

    @GetMapping
    public ResponseEntity<Map<String, Object>> search(
            HttpServletRequest request,
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        if (scope != null && scope.isEmpty()) {
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", List.of()));
        }
        String safe = sanitize(q);
        if (safe.isEmpty()) {
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", List.of()));
        }
        int lim = Math.min(Math.max(limit, 1), 50);
        List<Map<String, Object>> rows = orderMapper.quickSearch(safe, lim);
        if (scope != null && !scope.isEmpty()) {
            rows = rows.stream()
                    .filter(m -> scope.contains(String.valueOf(m.getOrDefault("merchantId", ""))))
                    .collect(Collectors.toList());
        }
        List<Map<String, Object>> shaped = rows.stream().map(this::shapeRow).collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", shaped));
    }

    private Map<String, Object> shapeRow(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "ORDER");
        m.put("orderId", row.get("orderId"));
        m.put("merchantId", row.get("merchantId"));
        m.put("merchantOrderNo", row.get("merchantOrderNo"));
        m.put("status", row.get("status"));
        m.put("amount", row.get("amount"));
        m.put("createdAt", row.get("createdAt"));
        return m;
    }

    private static String sanitize(String q) {
        if (q == null) {
            return "";
        }
        String t = q.trim().replace("%", "").replace("_", "");
        if (t.length() > 64) {
            return t.substring(0, 64);
        }
        return t;
    }
}
