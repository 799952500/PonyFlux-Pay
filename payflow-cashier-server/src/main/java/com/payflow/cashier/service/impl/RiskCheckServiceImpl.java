package com.payflow.cashier.service.impl;

import com.payflow.cashier.dto.CreateOrderRequest;
import com.payflow.cashier.entity.RiskRule;
import com.payflow.cashier.exception.BizException;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.service.RiskCheckService;
import com.payflow.cashier.service.RiskRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RiskCheckServiceImpl implements RiskCheckService {

    private static final String RULE_TYPE_AMOUNT_SINGLE = "AMOUNT_SINGLE";
    private static final String RULE_TYPE_AMOUNT_DAILY = "AMOUNT_DAILY";
    private static final String ACTION_REJECT = "REJECT";

    private final RiskRuleService riskRuleService;
    private final OrderMapper orderMapper;

    @Override
    public void checkCreateOrder(CreateOrderRequest request) {
        List<RiskRule> rules = riskRuleService.listEnabledRules();
        if (rules.isEmpty()) {
            return;
        }

        BigDecimal orderAmountYuan = centsToYuan(request.getAmount());

        for (RiskRule rule : rules) {
            if (RULE_TYPE_AMOUNT_SINGLE.equals(rule.getRuleType())) {
                checkSingleAmount(rule, orderAmountYuan);
            } else if (RULE_TYPE_AMOUNT_DAILY.equals(rule.getRuleType())) {
                checkDailyAmount(rule, request.getMerchantId(), orderAmountYuan);
            }
        }
    }

    private void checkSingleAmount(RiskRule rule, BigDecimal orderAmountYuan) {
        if (rule.getThreshold() == null) {
            return;
        }
        if (orderAmountYuan.compareTo(rule.getThreshold()) <= 0) {
            return;
        }
        if (ACTION_REJECT.equals(rule.getAction())) {
            throw new BizException(6101, String.format("风控拦截：单笔金额%.2f元超过阈值%.2f元",
                    orderAmountYuan, rule.getThreshold()));
        }
    }

    private void checkDailyAmount(RiskRule rule, String merchantId, BigDecimal orderAmountYuan) {
        if (rule.getThreshold() == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Long sumCents = orderMapper.sumAmountByMerchantAndTimeRange(merchantId, start, end);
        BigDecimal sumYuan = centsToYuan(sumCents == null ? 0L : sumCents);
        BigDecimal total = sumYuan.add(orderAmountYuan);

        if (total.compareTo(rule.getThreshold()) <= 0) {
            return;
        }
        if (ACTION_REJECT.equals(rule.getAction())) {
            throw new BizException(6102, String.format("风控拦截：当日累计金额%.2f元超过阈值%.2f元",
                    total, rule.getThreshold()));
        }
    }

    private BigDecimal centsToYuan(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}

