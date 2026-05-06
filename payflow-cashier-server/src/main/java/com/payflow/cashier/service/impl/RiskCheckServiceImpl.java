package com.payflow.cashier.service.impl;

import com.payflow.cashier.dto.CreateOrderRequest;
import com.payflow.cashier.entity.RiskRule;
import com.payflow.common.exception.BizException;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.service.RiskCheckService;
import com.payflow.cashier.service.RiskRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 风控校验服务实现。
 *
 * <p>所有金额统一使用「分」（Long）比较，避免 BigDecimal 浮点精度问题。
 * 风控规则阈值（threshold）存储单位同样为分。</p>
 *
 * @author Lucas
 */
@Slf4j
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

        long orderAmountCents = request.getAmount();

        for (RiskRule rule : rules) {
            if (RULE_TYPE_AMOUNT_SINGLE.equals(rule.getRuleType())) {
                checkSingleAmount(rule, orderAmountCents);
            } else if (RULE_TYPE_AMOUNT_DAILY.equals(rule.getRuleType())) {
                checkDailyAmount(rule, request.getMerchantId(), orderAmountCents);
            }
        }
    }

    /**
     * 校验单笔金额是否超过阈值。
     *
     * @param rule             风控规则
     * @param orderAmountCents 订单金额（分）
     */
    private void checkSingleAmount(RiskRule rule, long orderAmountCents) {
        Long thresholdCents = parseThresholdToCents(rule.getThreshold());
        if (thresholdCents == null) {
            return;
        }
        if (orderAmountCents <= thresholdCents) {
            return;
        }
        if (ACTION_REJECT.equals(rule.getAction())) {
            throw new BizException(6101, String.format("风控拦截：单笔金额%.2f元超过阈值%.2f元",
                    centsToYuan(orderAmountCents), centsToYuan(thresholdCents)));
        }
    }

    /**
     * 校验当日累计金额是否超过阈值。
     *
     * @param rule             风控规则
     * @param merchantId       商户ID
     * @param orderAmountCents 订单金额（分）
     */
    private void checkDailyAmount(RiskRule rule, String merchantId, long orderAmountCents) {
        Long thresholdCents = parseThresholdToCents(rule.getThreshold());
        if (thresholdCents == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        Long sumCents = orderMapper.sumAmountByMerchantAndTimeRange(merchantId, start, end);
        long dailyTotalCents = (sumCents == null ? 0L : sumCents) + orderAmountCents;

        if (dailyTotalCents <= thresholdCents) {
            return;
        }
        if (ACTION_REJECT.equals(rule.getAction())) {
            throw new BizException(6102, String.format("风控拦截：当日累计金额%.2f元超过阈值%.2f元",
                    centsToYuan(dailyTotalCents), centsToYuan(thresholdCents)));
        }
    }

    /**
     * 将规则阈值转换为分。
     *
     * <p>兼容两种存储格式：</p>
     * <ul>
     *     <li>新格式：threshold 为整数分（如 10000 表示 100元）</li>
     *     <li>旧格式：threshold 为元的小数（如 100.00 表示 100元），自动转换为分</li>
     * </ul>
     *
     * @param threshold 风控阈值
     * @return 阈值（分），null 表示未配置
     */
    private Long parseThresholdToCents(java.math.BigDecimal threshold) {
        if (threshold == null) {
            return null;
        }
        // 如果是整数值（无小数部分），按分处理
        if (threshold.scale() <= 0 || threshold.stripTrailingZeros().scale() <= 0) {
            return threshold.longValue();
        }
        // 如果有小数部分，按元处理，转换为分
        return threshold.multiply(java.math.BigDecimal.valueOf(100))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValue();
    }

    /**
     * 分转元（仅用于错误提示信息）。
     *
     * @param cents 金额（分）
     * @return 元
     */
    private static double centsToYuan(long cents) {
        return cents / 100.0;
    }
}
