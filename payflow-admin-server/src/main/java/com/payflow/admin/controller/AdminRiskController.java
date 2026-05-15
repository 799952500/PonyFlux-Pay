package com.payflow.admin.controller;

import com.payflow.admin.dto.UpdateRiskRuleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.RiskRule;
import com.payflow.admin.mapper.RiskRuleMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 风控规则管理 Controller
  * @author Lucas
 */
@Tag(name = "风控规则")
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
    @Operation(summary = "查询风控规则列表")
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
    @Operation(summary = "更新风控规则")
    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<Map<String, Object>> updateRule(@PathVariable Long ruleId,
                                                          @Valid @RequestBody UpdateRiskRuleRequest body) {
        RiskRule exist = riskRuleMapper.selectById(ruleId);
        if (exist == null) {
            return ResponseEntity.ok(Map.of(
                    "code", 404,
                    "message", "规则不存在",
                    "data", (Object) null
            ));
        }

        if (body.getEnabled() != null) {
            exist.setEnabled(body.getEnabled());
        }
        if (body.getThreshold() != null) {
            exist.setThreshold(body.getThreshold());
        }
        if (body.getUnit() != null) {
            exist.setUnit(body.getUnit());
        }
        if (body.getDescription() != null) {
            exist.setDescription(body.getDescription());
        }
        if (body.getRuleName() != null) {
            exist.setRuleName(body.getRuleName());
        }
        if (body.getAction() != null) {
            exist.setAction(body.getAction());
        }
        if (body.getRuleType() != null) {
            exist.setRuleType(body.getRuleType());
        }

        riskRuleMapper.updateById(exist);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 0);
        response.put("message", "success");
        response.put("data", exist);
        return ResponseEntity.ok(response);
    }
}
