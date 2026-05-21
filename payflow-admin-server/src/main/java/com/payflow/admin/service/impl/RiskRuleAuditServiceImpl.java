package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.RiskRule;
import com.payflow.admin.entity.RiskRuleAuditLog;
import com.payflow.admin.mapper.RiskRuleAuditLogMapper;
import com.payflow.admin.service.RiskRuleAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 风控规则审计服务实现。
 */
@Service
@RequiredArgsConstructor
public class RiskRuleAuditServiceImpl implements RiskRuleAuditService {

    private final RiskRuleAuditLogMapper mapper;

    @Override
    public void record(RiskRule rule, String operationType, String beforeSummary, String afterSummary, String operatorType, String operatorId, String operatorName, String merchantId, String clientIp) {
        mapper.insert(buildLog(rule, operationType, beforeSummary, afterSummary, operatorType, operatorId, operatorName, merchantId, clientIp));
    }

    @Override
    public Map<String, Object> pageAudits(Integer page, Integer pageSize, Long ruleId, String operatorType, String merchantId, String operationType, String startTime, String endTime) {
        int current = page == null ? 1 : page;
        int size = Math.min(pageSize == null ? 20 : pageSize, 100);
        LambdaQueryWrapper<RiskRuleAuditLog> wrapper = new LambdaQueryWrapper<RiskRuleAuditLog>()
                .eq(ruleId != null, RiskRuleAuditLog::getRuleId, ruleId)
                .eq(StringUtils.hasText(operatorType), RiskRuleAuditLog::getOperatorType, operatorType)
                .eq(StringUtils.hasText(merchantId), RiskRuleAuditLog::getMerchantId, merchantId)
                .eq(StringUtils.hasText(operationType), RiskRuleAuditLog::getOperationType, operationType)
                .ge(StringUtils.hasText(startTime), RiskRuleAuditLog::getCreatedAt, startTime)
                .le(StringUtils.hasText(endTime), RiskRuleAuditLog::getCreatedAt, endTime)
                .orderByDesc(RiskRuleAuditLog::getCreatedAt);
        Page<RiskRuleAuditLog> result = mapper.selectPage(Page.of(current, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords());
        data.put("total", result.getTotal());
        data.put("page", current);
        data.put("pageSize", size);
        return data;
    }

    @Override
    public RiskRuleAuditLog buildLog(RiskRule rule, String operationType, String beforeSummary, String afterSummary, String operatorType, String operatorId, String operatorName, String merchantId, String clientIp) {
        RiskRuleAuditLog log = new RiskRuleAuditLog();
        log.setRuleId(rule.getId());
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setOperatorType(operatorType);
        log.setMerchantId(merchantId);
        log.setOperationType(operationType);
        log.setBeforeSummary(beforeSummary);
        log.setAfterSummary(afterSummary);
        log.setClientIp(clientIp);
        return log;
    }
}
