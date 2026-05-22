package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.dto.RiskRuleQueryRequest;
import com.payflow.admin.dto.RiskRuleStatusRequest;
import com.payflow.admin.dto.RiskRuleUpsertRequest;
import com.payflow.admin.dto.RiskRuleVO;
import com.payflow.admin.entity.Merchant;
import com.payflow.admin.entity.RiskRule;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.common.exception.BizException;
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
    private static final String OWNER_MERCHANT = "MERCHANT";
    private static final String SCOPE_SELECTED = "SELECTED_MERCHANTS";
    private static final String SCOPE_OWNER_ONLY = "OWNER_MERCHANT_ONLY";
    private static final int CROSS_MERCHANT_DENIED = 6101;

    private final RiskRuleMapper riskRuleMapper;
    private final RiskRuleMerchantScopeMapper scopeMapper;
    private final MerchantMapper merchantMapper;
    private final RiskRuleAuditService auditService;
    private final Optional<CashierConfigRefreshPublisher> refreshPublisher;

    @Override
    public Map<String, Object> pageRules(RiskRuleQueryRequest request) {
        return pageRules(request, null);
    }

    @Override
    public Map<String, Object> pageRules(RiskRuleQueryRequest request, List<String> merchantScopeIds) {
        int page = request.getPage() == null ? 1 : request.getPage();
        int pageSize = Math.min(request.getPageSize() == null ? 20 : request.getPageSize(), 100);
        String scopedMerchantId = AdminRequestContext.resolveMerchantFilter(request.getMerchantId(), merchantScopeIds);
        if ("__NO_ACCESS__".equals(scopedMerchantId)) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("list", List.of());
            empty.put("total", 0L);
            empty.put("page", page);
            empty.put("pageSize", pageSize);
            return empty;
        }
        if (StringUtils.hasText(scopedMerchantId)) {
            request.setMerchantId(scopedMerchantId);
        }

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
        List<RiskRuleVO> visible = result.getRecords().stream()
                .filter(rule -> isRuleVisible(rule, merchantScopeIds))
                .map(this::toVO)
                .toList();
        Map<String, Object> data = new HashMap<>();
        data.put("list", visible);
        data.put("total", merchantScopeIds == null ? result.getTotal() : visible.size());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskRuleVO createRule(RiskRuleUpsertRequest request) {
        return createRule(request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskRuleVO createRule(RiskRuleUpsertRequest request, List<String> merchantScopeIds) {
        RiskRule rule = new RiskRule();
        if (merchantScopeIds == null) {
            applyPlatformRequest(rule, request);
            riskRuleMapper.insert(rule);
            replaceScopeRecords(rule.getId(), request.getScopeMerchantIds());
        } else {
            String merchantId = AdminRequestContext.resolveMerchantIdForWrite(
                    merchantScopeIds, request.getOwnerMerchantId());
            applyMerchantRequest(rule, request, merchantId);
            riskRuleMapper.insert(rule);
        }
        auditService.record(rule, "CREATE", null, summary(rule), "ADMIN", null, "admin", null, null);
        publishRefresh("risk_rule_create");
        return toVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskRuleVO updateRule(Long ruleId, RiskRuleUpsertRequest request) {
        return updateRule(ruleId, request, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RiskRuleVO updateRule(Long ruleId, RiskRuleUpsertRequest request, List<String> merchantScopeIds) {
        RiskRule rule = requireRule(ruleId);
        String before = summary(rule);
        if (merchantScopeIds == null) {
            if (!OWNER_PLATFORM.equals(rule.getOwnerType())) {
                throw new IllegalArgumentException("商户自建规则仅可查看，请由商户在其侧维护");
            }
            applyPlatformRequest(rule, request);
            riskRuleMapper.updateById(rule);
            replaceScopeRecords(ruleId, request.getScopeMerchantIds());
        } else {
            assertMerchantRuleWritable(rule, merchantScopeIds);
            String merchantId = AdminRequestContext.resolveMerchantIdForWrite(
                    merchantScopeIds, request.getOwnerMerchantId());
            applyMerchantRequest(rule, request, merchantId);
            riskRuleMapper.updateById(rule);
        }
        auditService.record(rule, "UPDATE", before, summary(rule), "ADMIN", null, "admin", null, null);
        publishRefresh("risk_rule_update");
        return toVO(rule);
    }

    @Override
    public RiskRuleVO updateStatus(Long ruleId, RiskRuleStatusRequest request) {
        return updateStatus(ruleId, request, null);
    }

    @Override
    public RiskRuleVO updateStatus(Long ruleId, RiskRuleStatusRequest request, List<String> merchantScopeIds) {
        RiskRule rule = requireRule(ruleId);
        if (merchantScopeIds != null && !merchantScopeIds.isEmpty()) {
            if (OWNER_PLATFORM.equals(rule.getOwnerType())) {
                throw new BizException(CROSS_MERCHANT_DENIED, "无权修改平台规则状态");
            }
            assertMerchantRuleWritable(rule, merchantScopeIds);
        }
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

    private void applyPlatformRequest(RiskRule rule, RiskRuleUpsertRequest request) {
        String ownerType = StringUtils.hasText(request.getOwnerType()) ? request.getOwnerType() : OWNER_PLATFORM;
        String scopeType = StringUtils.hasText(request.getScopeType()) ? request.getScopeType() : "ALL_MERCHANTS";
        if (!OWNER_PLATFORM.equals(ownerType)) {
            throw new IllegalArgumentException("平台管理员接口只能维护平台规则");
        }
        if (SCOPE_SELECTED.equals(scopeType) && (request.getScopeMerchantIds() == null || request.getScopeMerchantIds().isEmpty())) {
            throw new IllegalArgumentException("平台定向规则必须选择商户范围");
        }
        fillRuleFields(rule, request);
        rule.setOwnerType(OWNER_PLATFORM);
        rule.setOwnerMerchantId(null);
        rule.setScopeType(scopeType);
    }

    private void applyMerchantRequest(RiskRule rule, RiskRuleUpsertRequest request, String merchantId) {
        if (OWNER_PLATFORM.equals(request.getOwnerType()) || "ALL_MERCHANTS".equals(request.getScopeType())
                || SCOPE_SELECTED.equals(request.getScopeType())) {
            throw new BizException(CROSS_MERCHANT_DENIED, "商户管理员只能维护本商户风控规则");
        }
        fillRuleFields(rule, request);
        rule.setOwnerType(OWNER_MERCHANT);
        rule.setOwnerMerchantId(merchantId);
        rule.setScopeType(SCOPE_OWNER_ONLY);
    }

    private void fillRuleFields(RiskRule rule, RiskRuleUpsertRequest request) {
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
        rule.setDescription(request.getDescription());
    }

    private void assertMerchantRuleWritable(RiskRule rule, List<String> merchantScopeIds) {
        if (!OWNER_MERCHANT.equals(rule.getOwnerType())) {
            throw new BizException(CROSS_MERCHANT_DENIED, "无权修改平台规则");
        }
        AdminRequestContext.assertMerchantAllowed(rule.getOwnerMerchantId(), merchantScopeIds);
    }

    private boolean isRuleVisible(RiskRule rule, List<String> merchantScopeIds) {
        if (merchantScopeIds == null || merchantScopeIds.isEmpty()) {
            return true;
        }
        if ("ALL_MERCHANTS".equals(rule.getScopeType())) {
            return true;
        }
        if (StringUtils.hasText(rule.getOwnerMerchantId())
                && merchantScopeIds.contains(rule.getOwnerMerchantId())) {
            return true;
        }
        if (!SCOPE_SELECTED.equals(rule.getScopeType()) || rule.getId() == null) {
            return false;
        }
        Long count = scopeMapper.selectCount(new LambdaQueryWrapper<RiskRuleMerchantScope>()
                .eq(RiskRuleMerchantScope::getRuleId, rule.getId())
                .eq(RiskRuleMerchantScope::getEnabled, true)
                .in(RiskRuleMerchantScope::getMerchantId, merchantScopeIds));
        return count != null && count > 0;
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
