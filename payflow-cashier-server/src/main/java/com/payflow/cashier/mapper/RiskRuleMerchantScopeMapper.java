package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.RiskRuleMerchantScope;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收银台风控规则商户作用范围 Mapper。
 */
@Mapper
public interface RiskRuleMerchantScopeMapper extends BaseMapper<RiskRuleMerchantScope> {
}
