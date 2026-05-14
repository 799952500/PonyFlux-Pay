package com.payflow.admin.controller;

import com.payflow.admin.entity.ChurnAlert;
import com.payflow.admin.service.ChurnAlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 流失预警管理 Controller
 *
 * @author PayFlow Team
 */
@Tag(name = "流失预警")
@RestController
@RequestMapping("/api/v1/admin/churn-alerts")
@RequiredArgsConstructor
public class ChurnAlertController {

    private final ChurnAlertService churnAlertService;

    @Operation(summary = "预警列表")
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status) {
        var result = churnAlertService.getAlerts(page, size, merchantId, status);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of(
                        "list", result.getRecords(),
                        "total", result.getTotal(),
                        "page", result.getCurrent(),
                        "size", result.getSize()
                )
        ));
    }

    @Operation(summary = "预警详情")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        ChurnAlert alert = churnAlertService.getAlertDetail(id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", alert));
    }

    @Operation(summary = "更新预警状态")
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String assignee) {
        boolean ok = churnAlertService.updateAlertStatus(id, status, note, assignee);
        return ResponseEntity.ok(Map.of("code", ok ? 0 : 1, "message", ok ? "success" : "预警不存在"));
    }
}
