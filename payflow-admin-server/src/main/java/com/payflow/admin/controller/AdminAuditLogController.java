package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.entity.AdminAuditLog;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计日志查询。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> page(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        IPage<AdminAuditLog> p = auditLogService.page(
                page, pageSize, username, action, merchantId, resourceType, result, start, end,
                AdminRequestContext.merchantScope(request));
        Map<String, Object> data = new HashMap<>();
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
