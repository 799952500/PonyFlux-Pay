package com.payflow.admin.service;

import com.payflow.admin.entity.RiskRule;
import com.payflow.admin.entity.RiskRuleAuditLog;

import java.util.Map;

/**
 * 风控规则审计服务。
 */
public interface RiskRuleAuditService {

    void record(RiskRule rule, String operationType, String beforeSummary, String afterSummary, String operatorType, String operatorId, String operatorName, String merchantId, String clientIp);

    Map<String, Object> pageAudits(Integer page, Integer pageSize, Long ruleId, String operatorType, String merchantId, String operationType, String startTime, String endTime);

    RiskRuleAuditLog buildLog(RiskRule rule, String operationType, String beforeSummary, String afterSummary, String operatorType, String operatorId, String operatorName, String merchantId, String clientIp);
}
