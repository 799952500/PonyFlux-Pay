package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.RiskRule;
import com.payflow.admin.mapper.RiskRuleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控规则管理 Controller
 */
@RestController
@RequestMapping("/api/v1/admin/risk")
@RequiredArgsConstructor
public class AdminRiskController {

    private final RiskRuleMapper riskRuleMapper;

    /**
     * 查询所有风控规则列表
     *
     * @return 风控规则列表，按启用状态降序排列
     */
    @GetMapping("/rules")
    public ResponseEntity<Map<String, Object>> listRules() {
        List<RiskRule> rules = riskRuleMapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .orderByDesc(RiskRule::getEnabled)
                .orderByAsc(RiskRule::getId));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", rules);

        return ResponseEntity.ok(response);
    }

    /**
     * 更新指定风控规则
     *
     * @param ruleId 规则ID
     * @param body   待更新字段映射
     * @return 更新后的规则对象
     */
    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long ruleId,
                                                          @RequestBody Map<String, Object> body) {
        RiskRule exist = riskRuleMapper.selectById(ruleId);
        if (exist == null) {
            return ResponseEntity.ok(Map.of(
                    "code", 404,
                    "message", "规则不存在",
                    "data", (Object) null
            ));
        }

        if (body.containsKey("enabled")) {
            Object enabled = body.get("enabled");
            exist.setEnabled(enabled != null && (Boolean) enabled);
        }
        if (body.containsKey("threshold")) {
            Object threshold = body.get("threshold");
            if (threshold == null) {
                exist.setThreshold(null);
            } else {
                exist.setThreshold(new BigDecimal(String.valueOf(threshold)));
            }
        }
        if (body.containsKey("unit")) {
            exist.setUnit((String) body.get("unit"));
        }
        if (body.containsKey("description")) {
            exist.setDescription((String) body.get("description"));
        }
        if (body.containsKey("ruleName")) {
            exist.setRuleName((String) body.get("ruleName"));
        }
        if (body.containsKey("action")) {
            exist.setAction((String) body.get("action"));
        }
        if (body.containsKey("ruleType")) {
            exist.setRuleType((String) body.get("ruleType"));
        }

        riskRuleMapper.updateById(exist);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", exist);
        return ResponseEntity.ok(response);
    }
}
