package com.payflow.cashier.service;

import com.payflow.cashier.dto.PaymentRiskContext;
import com.payflow.cashier.entity.RiskRule;

/**
 * 风控命中记录写入服务。
 */
public interface RiskHitRecordService {

    void recordHit(PaymentRiskContext context, RiskRule rule, String decision, String hitReason);
}
