package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.entity.cashier.SecurityAuditEntity;
import com.payflow.admin.security.RequireRole;
import com.payflow.admin.service.AdminSecurityAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 商户安全审计查询（越权拒绝记录）。
 *
 * @author PayFlow Team
 */
@RestController
@RequestMapping("/api/v1/admin/security/audit")
@RequiredArgsConstructor
public class AdminSecurityAuditController {

    private final AdminSecurityAuditService adminSecurityAuditService;

    @GetMapping
    @RequireRole({RequireRole.RISK, RequireRole.SUPER_ADMIN})
    public ResponseEntity<Map<String, Object>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) String reasonCode,
            @RequestParam(required = false) String requestPath,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        IPage<SecurityAuditEntity> p = adminSecurityAuditService.page(
                page, pageSize, merchantId, outcome, reasonCode, requestPath, start, end);
        Map<String, Object> data = new HashMap<>();
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", page);
        data.put("pageSize", Math.min(Math.max(pageSize, 1), 100));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
