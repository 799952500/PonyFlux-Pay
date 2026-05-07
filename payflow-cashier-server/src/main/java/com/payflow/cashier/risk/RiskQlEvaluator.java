package com.payflow.cashier.risk;

import com.payflow.cashier.dto.CreateOrderRequest;
import com.payflow.cashier.entity.RiskRule;
import com.payflow.common.exception.BizException;
import com.ql.util.express.DefaultContext;
import com.ql.util.express.ExpressRunner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 风控 QLExpress 规则执行器：表达式返回 Boolean，true 表示放行，false 表示拦截（结合规则的 action）。
 */
@Slf4j
@Component
public class RiskQlEvaluator {

    private final ExpressRunner runner = new ExpressRunner();

    /**
     * @return true 放行；false 表示表达式判定为不通过
     */
    public boolean evaluateAllow(RiskRule rule, CreateOrderRequest request) {
        String expr = rule.getRiskExpr();
        if (expr == null || expr.isBlank()) {
            return true;
        }
        try {
            DefaultContext<String, Object> ctx = new DefaultContext<>();
            ctx.put("merchantId", request.getMerchantId());
            ctx.put("amount", request.getAmount());
            ctx.put("clientIp", request.getClientIp());
            ctx.put("channel", request.getChannel());
            Object out = runner.execute(expr, ctx, null, true, false);
            if (out instanceof Boolean b) {
                return b;
            }
            throw new BizException(6103, "风控表达式必须返回 Boolean: ruleCode=" + rule.getRuleCode());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("风控表达式执行失败: ruleCode={}", rule.getRuleCode(), e);
            throw new BizException(6103, "风控表达式执行失败: " + e.getMessage());
        }
    }
}
