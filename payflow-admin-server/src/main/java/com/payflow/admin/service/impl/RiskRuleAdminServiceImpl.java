package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.dto.RiskRuleQueryRequest;
import com.payflow.admin.dto.RiskRuleStatusRequest;
import com.payflow.admin.dto.RiskRuleUpsertRequest;
import com.payflow.admin.dto.RiskRuleVO;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.entity.RiskRule;
import com.payflow.admin.entity.RiskRuleMerchantScope;
import com.payflow.admin.mapper.MerchantMapper;
import com.payflow.admin.mapper.RiskRuleMapper;
import com.payflow.admin.mapper.RiskRuleMerchantScopeMapper;
import com.payflow.admin.redis.CashierConfigRefreshPublisher;
import com.payflow.admin.service.RiskRuleAdminService;
import com.payflow.admin.service.RiskRuleAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 管理端风控规则服务实现。
 */
@Service
@RequiredArgsConstructor
public class RiskRuleAdminServiceImpl implements RiskRuleAdminService {

    private static final String OWNER_PLATFORM = "PLATFORM";
    private static final String SCOPE_SELECTED = "SELECTED_MERCHANTS";

    private final RiskRuleMapper riskRuleMapper;
    private final RiskRuleMerchantScopeMapper scopeMapper;
    private final MerchantMapper merchantMapper;
    private final RiskRuleAuditService auditService;
    private final Optional<CashierConfigRefreshPublisher> refreshPublisher;

    @Override
    public Map<String, Object> pageRules(RiskRuleQueryRequest request) {
        int page = request.getPage() == null ? 1 : request.getPage();
        int pageSize = Math.min(request.getPageSize() == null ? 20 : request.getPageSize(), 100);

        LambdaQueryWrapper<RiskRule> wrapper = new LambdaQueryWrapper<RiskRule>()
                .eq(StringUtils.hasText(request.getOwnerType()), RiskRule::getOwnerType, request.getOwnerType())
                .eq(StringUtils.hasText(request.getScopeType()), RiskRule::getScopeType, request.getScopeType())
                .eq(StringUtils.hasText(request.getRuleType()), RiskRule::getRuleType, request.getRuleType())
                .eq(request.getEnabled() != null, RiskRule::getEnabled, request.getEnabled())
                .and(StringUtils.hasText(request.getMerchantId()), w -> w
                        .eq(RiskRule::getOwnerMerchantId, request.getMerchantId())
                        .or()
                        .eq(RiskRule::getScopeType, "ALL_MERCHANTS"))
                .and(StringUtils.hasText(request.getKeyword()), w -> w
                        .like(RiskRule::getRuleCode, request.getKeyword())
                        .or()
                        .like(RiskRule::getRuleName, request.getKeyword()))
                .orderByAsc(RiskRule::getPriority)
                .orderByDesc(RiskRule::getUpdatedAt);

        Page<RiskRule> result = riskRuleMapper.selectPage(Page.of(page, pageSize), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("list", result.getRecords().stream().map(this::toVO).toList());
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskRuleVO createRule(RiskRuleUpsertRequest request) {
        RiskRule rule = new RiskRule();
        applyRequest(rule, request);
        riskRuleMapper.insert(rule);
        replaceScopeRecords(rule.getId(), request.getScopeMerchantIds());
        auditService.record(rule, "CREATE", null, summary(rule), "ADMIN", null, "admin", null, null);
        publishRefresh("risk_rule_create");
        return toVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskRuleVO updateRule(Long ruleId, RiskRuleUpsertRequest request) {
        RiskRule rule = requireRule(ruleId);
        if (!OWNER_PLATFORM.equals(rule.getOwnerType())) {
            throw new IllegalArgumentException("商户自建规则仅可查看，请由商户在其侧维护");
        }
        String before = summary(rule);
        applyRequest(rule, request);
        riskRuleMapper.updateById(rule);
        replaceScopeRecords(ruleId, request.getScopeMerchantIds());
        auditService.record(rule, "UPDATE", before, summary(rule), "ADMIN", null, "admin", null, null);
        publishRefresh("risk_rule_update");
        return toVO(rule);
    }

    @Override
    public RiskRuleVO updateStatus(Long ruleId, RiskRuleStatusRequest request) {
        RiskRule rule = requireRule(ruleId);
        String before = summary(rule);
        rule.setEnabled(request.getEnabled());
        riskRuleMapper.updateById(rule);
        auditService.record(rule, Boolean.TRUE.equals(request.getEnabled()) ? "ENABLE" : "DISABLE", before, summary(rule), "ADMIN", null, "admin", null, null);
        publishRefresh("risk_rule_status");
        return toVO(rule);
    }

    @Override
    public Map<String, Object> getScopes(Long ruleId) {
        RiskRule rule = requireRule(ruleId);
        List<RiskRuleMerchantScope> scopes = scopeMapper.selectList(new LambdaQueryWrapper<RiskRuleMerchantScope>()
                .eq(RiskRuleMerchantScope::getRuleId, ruleId)
                .eq(RiskRuleMerchantScope::getEnabled, true));
        Map<String, Object> data = new HashMap<>();
        data.put("ruleId", ruleId);
        data.put("scopeType", rule.getScopeType());
        data.put("merchants", scopes);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> replaceScopes(Long ruleId, List<String> merchantIds) {
        RiskRule rule = requireRule(ruleId);
        if (!OWNER_PLATFORM.equals(rule.getOwnerType()) || !SCOPE_SELECTED.equals(rule.getScopeType())) {
            throw new IllegalArgumentException("仅平台定向规则允许维护商户范围");
        }
        replaceScopeRecords(ruleId, merchantIds);
        auditService.record(rule, "SCOPE_CHANGE", null, "scopeMerchantIds=" + merchantIds, "ADMIN", null, "admin", null, null);
        publishRefresh("risk_rule_scope");
        return getScopes(ruleId);
    }

    private void applyRequest(RiskRule rule, RiskRuleUpsertRequest request) {
        String ownerType = StringUtils.hasText(request.getOwnerType()) ? request.getOwnerType() : OWNER_PLATFORM;
        String scopeType = StringUtils.hasText(request.getScopeType()) ? request.getScopeType() : "ALL_MERCHANTS";
        if (!OWNER_PLATFORM.equals(ownerType)) {
            throw new IllegalArgumentException("管理员接口只能维护平台规则");
        }
        if (SCOPE_SELECTED.equals(scopeType) && (request.getScopeMerchantIds() == null || request.getScopeMerchantIds().isEmpty())) {
            throw new IllegalArgumentException("平台定向规则必须选择商户范围");
        }
        rule.setRuleCode(request.getRuleCode());
        rule.setRuleName(request.getRuleName());
        rule.setRuleType(request.getRuleType());
        rule.setRiskExpr(request.getRiskExpr());
        rule.setThresholdFen(request.getThresholdFen());
        rule.setThreshold(request.getThresholdFen() == null ? null : BigDecimal.valueOf(request.getThresholdFen()));
        rule.setUnit(request.getUnit());
        rule.setAction(request.getAction());
        rule.setEnabled(request.getEnabled());
        rule.setPriority(request.getPriority());
        rule.setOwnerType(ownerType);
        rule.setOwnerMerchantId(null);
        rule.setScopeType(scopeType);
        rule.setDescription(request.getDescription());
    }

    private RiskRule requireRule(Long ruleId) {
        RiskRule rule = riskRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new IllegalArgumentException("风控规则不存在");
        }
        return rule;
    }

    private void replaceScopeRecords(Long ruleId, List<String> merchantIds) {
        scopeMapper.delete(new LambdaQueryWrapper<RiskRuleMerchantScope>().eq(RiskRuleMerchantScope::getRuleId, ruleId));
        List<String> ids = merchantIds == null ? Collections.emptyList() : merchantIds.stream()
                .filter(Objects::nonNull)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        for (String merchantId : ids) {
            RiskRuleMerchantScope scope = new RiskRuleMerchantScope();
            scope.setRuleId(ruleId);
            scope.setMerchantId(merchantId);
            scope.setEnabled(true);
            scopeMapper.insert(scope);
        }
    }

    private RiskRuleVO toVO(RiskRule rule) {
        RiskRuleVO vo = new RiskRuleVO();
        vo.setId(rule.getId());
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleName(rule.getRuleName());
        vo.setRuleType(rule.getRuleType());
        vo.setRiskExpr(rule.getRiskExpr());
        vo.setThresholdFen(rule.getThresholdFen());
        vo.setUnit(rule.getUnit());
        vo.setAction(rule.getAction());
        vo.setEnabled(rule.getEnabled());
        vo.setPriority(rule.getPriority());
        vo.setOwnerType(rule.getOwnerType());
        vo.setOwnerMerchantId(rule.getOwnerMerchantId());
        if (StringUtils.hasText(rule.getOwnerMerchantId())) {
            Merchant merchant = merchantMapper.selectOne(new LambdaQueryWrapper<Merchant>()
                    .eq(Merchant::getMerchantId, rule.getOwnerMerchantId())
                    .last("LIMIT 1"));
            if (merchant != null) {
                vo.setOwnerMerchantName(merchant.getMerchantName());
            }
        }
        vo.setScopeType(rule.getScopeType());
        vo.setDescription(rule.getDescription());
        vo.setCreatedAt(rule.getCreatedAt());
        vo.setUpdatedAt(rule.getUpdatedAt());
        vo.setScopeMerchantCount(countScopes(rule));
        return vo;
    }

    private int countScopes(RiskRule rule) {
        if (!SCOPE_SELECTED.equals(rule.getScopeType()) || rule.getId() == null) {
            return 0;
        }
        return Math.toIntExact(scopeMapper.selectCount(new LambdaQueryWrapper<RiskRuleMerchantScope>()
                .eq(RiskRuleMerchantScope::getRuleId, rule.getId())
                .eq(RiskRuleMerchantScope::getEnabled, true)));
    }

    private void publishRefresh(String reason) {
        refreshPublisher.ifPresent(publisher -> publisher.publish(reason));
    }

    private String summary(RiskRule rule) {
        return "ruleCode=" + rule.getRuleCode()
                + ", ruleType=" + rule.getRuleType()
                + ", thresholdFen=" + rule.getThresholdFen()
                + ", enabled=" + rule.getEnabled()
                + ", scopeType=" + rule.getScopeType();
    }
}
