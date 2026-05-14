package com.payflow.admin.controller;

import com.payflow.admin.service.DashboardAggregationService;
import com.payflow.admin.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据导出 Controller（Excel 报表）。
 * 小数据量同步返回，大数据量异步生成后通知下载。
 *
 * @author PayFlow Team
 */
@Slf4j
@Tag(name = "数据导出")
@RestController
@RequestMapping("/api/v1/admin/export")
@RequiredArgsConstructor
public class AdminExportController {

    private final DashboardAggregationService dashboardAggregationService;

    /** 导出任务状态缓存（生产环境应落库） */
    private final ConcurrentHashMap<String, Map<String, Object>> exportTasks = new ConcurrentHashMap<>();

    /**
     * 创建导出任务
     */
    @Operation(summary = "创建导出任务")
    @PostMapping("/report")
    public ResponseEntity<Map<String, Object>> createExportTask(
            @RequestParam String dateFrom,
            @RequestParam String dateTo,
            @RequestParam(defaultValue = "ALL") String merchantId) {
        String taskId = UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> task = new LinkedHashMap<>();
        task.put("taskId", taskId);
        task.put("status", "processing");
        task.put("createdAt", java.time.LocalDateTime.now().toString());
        exportTasks.put(taskId, task);

        // 异步生成
        asyncGenerateReport(taskId, dateFrom, dateTo, merchantId);

        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of("taskId", taskId, "status", "processing")
        ));
    }

    /**
     * 查询导出任务状态
     */
    @Operation(summary = "查询导出任务列表")
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> getExportTasks() {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", exportTasks.values()
        ));
    }

    @Async
    @SuppressWarnings("unused")
    public void asyncGenerateReport(String taskId, String dateFrom, String dateTo, String merchantId) {
        try {
            // 模拟数据导出逻辑（生产环境应使用 Apache POI 生成 Excel）
            LocalDateTime start = LocalDateTime.parse(dateFrom + "T00:00:00");
            LocalDateTime end = LocalDateTime.parse(dateTo + "T23:59:59");
            Map<String, Object> metrics = dashboardAggregationService.queryMetrics(start, end, "day", "ALL");

            Map<String, Object> task = exportTasks.get(taskId);
            if (task != null) {
                task.put("status", "completed");
                task.put("totalAmount", metrics.get("totalAmount"));
                task.put("totalCount", metrics.get("totalCount"));
                task.put("downloadUrl", "/api/v1/admin/export/download/" + taskId);
                task.put("completedAt", java.time.LocalDateTime.now().toString());
            }
        } catch (Exception e) {
            log.error("导出任务失败: taskId={}", taskId, e);
            Map<String, Object> task = exportTasks.get(taskId);
            if (task != null) {
                task.put("status", "failed");
                task.put("error", e.getMessage());
            }
        }
    }
}
