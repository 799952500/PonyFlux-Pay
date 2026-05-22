package com.payflow.admin.service;

import com.payflow.admin.dto.RiskRuleQueryRequest;
import com.payflow.admin.dto.RiskRuleStatusRequest;
import com.payflow.admin.dto.RiskRuleUpsertRequest;
import com.payflow.admin.dto.RiskRuleVO;

import java.util.List;
import java.util.Map;

/**
 * 管理端风控规则服务。
 */
public interface RiskRuleAdminService {

    Map<String, Object> pageRules(RiskRuleQueryRequest request);

    Map<String, Object> pageRules(RiskRuleQueryRequest request, List<String> merchantScopeIds);

    RiskRuleVO createRule(RiskRuleUpsertRequest request);

    RiskRuleVO createRule(RiskRuleUpsertRequest request, List<String> merchantScopeIds);

    RiskRuleVO updateRule(Long ruleId, RiskRuleUpsertRequest request);

    RiskRuleVO updateRule(Long ruleId, RiskRuleUpsertRequest request, List<String> merchantScopeIds);

    RiskRuleVO updateStatus(Long ruleId, RiskRuleStatusRequest request);

    RiskRuleVO updateStatus(Long ruleId, RiskRuleStatusRequest request, List<String> merchantScopeIds);

    Map<String, Object> getScopes(Long ruleId);

    Map<String, Object> replaceScopes(Long ruleId, List<String> merchantIds);
}
