package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.entity.AdminAuditLog;
import com.payflow.admin.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        IPage<AdminAuditLog> p = auditLogService.page(page, pageSize);
        Map<String, Object> data = new HashMap<>();
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
