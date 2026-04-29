package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.RiskRule;
import com.payflow.cashier.mapper.RiskRuleMapper;
import com.payflow.cashier.service.RiskRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class RiskRuleServiceImpl implements RiskRuleService {

    private final RiskRuleMapper mapper;

    @Override
    public List<RiskRule> listEnabledRules() {
        return mapper.selectList(new LambdaQueryWrapper<RiskRule>()
                .eq(RiskRule::getEnabled, true)
                .orderByAsc(RiskRule::getId));
    }
}

