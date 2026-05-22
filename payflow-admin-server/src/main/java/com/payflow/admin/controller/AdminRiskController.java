package com.payflow.admin.controller;

import com.payflow.admin.dto.RiskRuleQueryRequest;
import com.payflow.admin.dto.RiskRuleStatusRequest;
import com.payflow.admin.dto.RiskRuleUpsertRequest;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.service.RiskHitRecordQueryService;
import jakarta.servlet.http.HttpServletRequest;
import com.payflow.admin.service.RiskRuleAdminService;
import com.payflow.admin.service.RiskRuleAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控规则管理 Controller。
 *
 * @author Lucas
 */
@Tag(name = "风控规则")
@RestController
@RequestMapping("/api/v1/admin/risk")
@RequiredArgsConstructor
public class AdminRiskController {

    private final RiskRuleAdminService riskRuleAdminService;
    private final RiskHitRecordQueryService riskHitRecordQueryService;
    private final RiskRuleAuditService riskRuleAuditService;

    @Operation(summary = "查询风控规则列表")
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> listRules(
            HttpServletRequest request,
            @Valid RiskRuleQueryRequest query) {
        return ok(riskRuleAdminService.pageRules(query, AdminRequestContext.merchantScope(request)));
    }

    @Operation(summary = "创建风控规则")
    @PostMapping("/rules")
    public ResponseEntity<Map<String, Object>> createRule(
            HttpServletRequest request,
            @Valid @RequestBody RiskRuleUpsertRequest body) {
        return ok(riskRuleAdminService.createRule(body, AdminRequestContext.merchantScope(request)));
    }

    @Operation(summary = "更新风控规则")
    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<Map<String, Object>> updateRule(
            HttpServletRequest request,
            @PathVariable Long ruleId,
            @Valid @RequestBody RiskRuleUpsertRequest body) {
        return ok(riskRuleAdminService.updateRule(ruleId, body, AdminRequestContext.merchantScope(request)));
    }

    @Operation(summary = "启用或停用风控规则")
    @PutMapping("/rules/{ruleId}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            HttpServletRequest request,
            @PathVariable Long ruleId,
            @Valid @RequestBody RiskRuleStatusRequest body) {
        return ok(riskRuleAdminService.updateStatus(ruleId, body, AdminRequestContext.merchantScope(request)));
    }

    @Operation(summary = "查询平台定向规则商户范围")
    @GetMapping("/rules/{ruleId}/scopes")
    public ResponseEntity<Map<String, Object>> getScopes(@PathVariable Long ruleId) {
        return ok(riskRuleAdminService.getScopes(ruleId));
    }

    @Operation(summary = "替换平台定向规则商户范围")
    @PutMapping("/rules/{ruleId}/scopes")
    public ResponseEntity<Map<String, Object>> replaceScopes(@PathVariable Long ruleId,
                                                             @RequestBody Map<String, List<String>> request) {
        return ok(riskRuleAdminService.replaceScopes(ruleId, request.getOrDefault("scopeMerchantIds", List.of())));
    }

    @Operation(summary = "查询风控命中记录")
    @GetMapping("/hits")
    public ResponseEntity<Map<String, Object>> listHits(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer page,
                                                        @RequestParam(defaultValue = "20") Integer pageSize,
                                                        @RequestParam(required = false) String merchantId,
                                                        @RequestParam(required = false) Long ruleId,
                                                        @RequestParam(required = false) String ownerType,
                                                        @RequestParam(required = false) String decision,
                                                        @RequestParam(required = false) String startTime,
                                                        @RequestParam(required = false) String endTime) {
        return ok(riskHitRecordQueryService.pageAdminHits(
                page, pageSize, merchantId, ruleId, ownerType, decision, startTime, endTime,
                AdminRequestContext.merchantScope(request)));
    }

    @Operation(summary = "查询风控规则审计")
    @GetMapping("/audits")
    public ResponseEntity<Map<String, Object>> listAudits(@RequestParam(defaultValue = "1") Integer page,
                                                          @RequestParam(defaultValue = "20") Integer pageSize,
                                                          @RequestParam(required = false) Long ruleId,
                                                          @RequestParam(required = false) String operatorType,
                                                          @RequestParam(required = false) String merchantId,
                                                          @RequestParam(required = false) String operationType,
                                                          @RequestParam(required = false) String startTime,
                                                          @RequestParam(required = false) String endTime) {
        return ok(riskRuleAuditService.pageAudits(page, pageSize, ruleId, operatorType, merchantId, operationType, startTime, endTime));
    }

    private ResponseEntity<Map<String, Object>> ok(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", data);
        return ResponseEntity.ok(response);
    }
}
