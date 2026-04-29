package com.payflow.cashier.service;

import com.payflow.cashier.entity.RiskRule;

import java.util.List;
/**
 * @author Lucas
 */

public interface RiskRuleService {
    List<RiskRule> listEnabledRules();
}

