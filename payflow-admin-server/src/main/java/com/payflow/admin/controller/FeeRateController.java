package com.payflow.admin.controller;

import com.payflow.admin.entity.FeeRateConfig;
import com.payflow.admin.entity.FeeRateAuditLog;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.service.FeeRateService;
import jakarta.servlet.http.HttpServletRequest;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 阶梯费率管理 Controller
 *
 * @author PayFlow Team
 */
@Tag(name = "阶梯费率")
@RestController
@RequestMapping("/api/v1/admin/fee-rates")
@RequiredArgsConstructor
public class FeeRateController {

    private final FeeRateService feeRateService;

    @Operation(summary = "费率规则列表")
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> list(HttpServletRequest request) {
        List<FeeRateConfig> rules = feeRateService.getAllRules(AdminRequestContext.merchantScope(request));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", rules));
    }

    @Operation(summary = "创建费率规则")
    @PostMapping("")
    public ResponseEntity<Map<String, Object>> create(@Valid @RequestBody FeeRateConfig config) {
        FeeRateConfig created = feeRateService.createRule(config);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", created));
    }

    @Operation(summary = "更新费率规则")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody FeeRateConfig config) {
        boolean ok = feeRateService.updateRule(id, config);
        return ResponseEntity.ok(Map.of("code", ok ? 0 : 1, "message", ok ? "success" : "规则不存在"));
    }

    @Operation(summary = "删除费率规则")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        boolean ok = feeRateService.deleteRule(id);
        return ResponseEntity.ok(Map.of("code", ok ? 0 : 1, "message", ok ? "success" : "规则不存在"));
    }

    @Operation(summary = "费率变更审计日志")
    @GetMapping("/audit-log")
    public ResponseEntity<Map<String, Object>> auditLog(
            HttpServletRequest request,
            @RequestParam(required = false) Long merchantId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<FeeRateAuditLog> logs = feeRateService.getAuditLogs(
                merchantId, page, size, AdminRequestContext.merchantScope(request));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", logs));
    }
}
