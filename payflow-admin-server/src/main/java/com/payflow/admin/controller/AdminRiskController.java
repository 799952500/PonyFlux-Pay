package com.payflow.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/risk")
@RequiredArgsConstructor
public class AdminRiskController {

    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> listRules() {
        List<Map<String, Object>> rules = new ArrayList<>();

        Map<String, Object> rule1 = new LinkedHashMap<>();
        rule1.put("ruleId", 1L);
        rule1.put("ruleCode", "MAX_AMOUNT");
        rule1.put("ruleName", "单笔最大金额");
        rule1.put("ruleType", "AMOUNT");
        rule1.put("threshold", new BigDecimal("50000.00"));
        rule1.put("action", "REJECT");
        rule1.put("enabled", true);
        rules.add(rule1);

        Map<String, Object> rule2 = new LinkedHashMap<>();
        rule2.put("ruleId", 2L);
        rule2.put("ruleCode", "DAILY_LIMIT");
        rule2.put("ruleName", "单日累计金额");
        rule2.put("ruleType", "AMOUNT");
        rule2.put("threshold", new BigDecimal("200000.00"));
        rule2.put("action", "REJECT");
        rule2.put("enabled", true);
        rules.add(rule2);

        Map<String, Object> rule3 = new LinkedHashMap<>();
        rule3.put("ruleId", 3L);
        rule3.put("ruleCode", "HIGH_RISK_IP");
        rule3.put("ruleName", "高风险IP拦截");
        rule3.put("ruleType", "IP");
        rule3.put("threshold", null);
        rule3.put("action", "REVIEW");
        rule3.put("enabled", false);
        rules.add(rule3);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", rules);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long ruleId, @RequestBody Map<String, Object> body) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", Map.of("ruleId", ruleId, "updated", true));
        return ResponseEntity.ok(response);
    }
}
