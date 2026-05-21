package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.RiskRule;
import com.payflow.cashier.entity.RiskRuleMerchantScope;
import com.payflow.cashier.mapper.RiskRuleMapper;
import com.payflow.cashier.mapper.RiskRuleMerchantScopeMapper;
import com.payflow.cashier.service.RiskRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 风控规则服务实现。
 *
 * @author Lucas
 */
@Service
@RequiredArgsConstructor
public class RiskRuleServiceImpl implements RiskRuleService {

    private static final String OWNER_PLATFORM = "PLATFORM";
    private static final String OWNER_MERCHANT = "MERCHANT";
    private static final String SCOPE_ALL = "ALL_MERCHANTS";
    private static final String SCOPE_SELECTED = "SELECTED_MERCHANTS";
    private static final String SCOPE_OWNER_ONLY = "OWNER_MERCHANT_ONLY";

    private final RiskRuleMapper mapper;
    private final RiskRuleMerchantScopeMapper scopeMapper;

    @Override
    public List<RiskRule> listEnabledRules() {
        return mapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getEnabled, true)
                .orderByAsc(RiskRule::getPriority)
                .orderByAsc(RiskRule::getId));
    }

    @Override
    public List<RiskRule> listEnabledRulesForMerchant(String merchantId) {
        if (!StringUtils.hasText(merchantId)) {
            return List.of();
        }
        Set<Long> selectedRuleIds = scopeMapper.selectList(new LambdaQueryWrapper<RiskRuleMerchantScope>()
                        .eq(RiskRuleMerchantScope::getMerchantId, merchantId)
                        .eq(RiskRuleMerchantScope::getEnabled, true))
                .stream()
                .map(RiskRuleMerchantScope::getRuleId)
                .collect(Collectors.toSet());

        List<RiskRule> rules = mapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getEnabled, true)
                .and(w -> w
                        .eq(RiskRule::getOwnerType, OWNER_PLATFORM).eq(RiskRule::getScopeType, SCOPE_ALL)
                        .or()
                        .eq(RiskRule::getOwnerType, OWNER_MERCHANT).eq(RiskRule::getOwnerMerchantId, merchantId).eq(RiskRule::getScopeType, SCOPE_OWNER_ONLY)
                        .or(!selectedRuleIds.isEmpty(), x -> x.eq(RiskRule::getOwnerType, OWNER_PLATFORM).eq(RiskRule::getScopeType, SCOPE_SELECTED).in(RiskRule::getId, selectedRuleIds)))
                .orderByAsc(RiskRule::getPriority)
                .orderByAsc(RiskRule::getId));
        return rules.stream()
                .filter(rule -> !SCOPE_SELECTED.equals(rule.getScopeType()) || selectedRuleIds.contains(rule.getId()))
                .toList();
    }
}

