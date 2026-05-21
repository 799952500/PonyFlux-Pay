package com.payflow.cashier.service;

import com.payflow.cashier.entity.RiskRule;

import java.util.List;

/**
 * 风控规则服务。
 *
 * @author Lucas
 */
public interface RiskRuleService {

    List<RiskRule> listEnabledRules();

    List<RiskRule> listEnabledRulesForMerchant(String merchantId);
}

