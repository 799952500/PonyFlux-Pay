package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.dto.DataIsolationCheckDTO;
import com.payflow.admin.dto.DataIsolationCheckQueryDTO;
import com.payflow.admin.dto.MerchantScopeDTO;
import com.payflow.admin.service.AdminMerchantScopeService;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.service.DataIsolationCheckService;
import com.payflow.admin.service.DataIsolationInventoryScanService;
import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据隔离治理检查项查询与扫描。
 */
@RestController
@RequestMapping("/api/v1/admin/data-isolation")
@RequiredArgsConstructor
public class DataIsolationCheckController {

    private static final int FORBIDDEN = 6101;

    private final DataIsolationCheckService dataIsolationCheckService;
    private final DataIsolationInventoryScanService dataIsolationInventoryScanService;
    private final AdminMerchantScopeService adminMerchantScopeService;

    @GetMapping("/checks")
    public ResponseEntity<Map<String, Object>> pageChecks(
            HttpServletRequest request,
            DataIsolationCheckQueryDTO query) {
        MerchantScopeDTO scope = adminMerchantScopeService.resolve(request);
        IPage<DataIsolationCheckDTO> page = dataIsolationCheckService.page(query, scope);
        Map<String, Object> data = new HashMap<>();
        data.put("list", page.getRecords());
        data.put("total", page.getTotal());
        data.put("page", page.getCurrent());
        data.put("size", page.getSize());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    @PostMapping("/checks/scan")
    public ResponseEntity<Map<String, Object>> triggerScan(HttpServletRequest request) {
        MerchantScopeDTO scope = adminMerchantScopeService.resolve(request);
        if (!scope.isPlatformAdmin()) {
            throw new BizException(FORBIDDEN, "无权执行隔离扫描");
        }
        int updated = dataIsolationInventoryScanService.runFullScan();
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of("updatedCount", updated)));
    }

    @RequirePermission("data_isolation:remediate")
    @PutMapping("/checks/{checkId}/remediation")
    public ResponseEntity<Map<String, Object>> updateRemediation(
            HttpServletRequest request,
            @PathVariable String checkId,
            @RequestBody Map<String, String> body) {
        MerchantScopeDTO scope = adminMerchantScopeService.resolve(request);
        if (!scope.isPlatformAdmin()) {
            throw new BizException(FORBIDDEN, "无权更新整改状态");
        }
        String status = body != null ? body.get("remediationStatus") : null;
        String reason = body != null ? body.get("decisionReason") : null;
        if (!StringUtils.hasText(status)) {
            throw new BizException(400, "remediationStatus 不能为空");
        }
        DataIsolationCheckDTO updated = dataIsolationCheckService.updateRemediation(checkId, status, reason, scope);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", updated));
    }
}
