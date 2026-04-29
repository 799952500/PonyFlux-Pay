package com.payflow.cashier.config;

import com.payflow.payment.core.PayStrategy;
import com.payflow.payment.core.PayStrategyRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 支付 SDK 注册：由渠道子模块提供 {@link PayStrategy} Bean，此处汇总为注册表。
 * @author Lucas
 */
@Configuration
public class PaymentSdkConfiguration {

    @Bean
    public PayStrategyRegistry payStrategyRegistry(List<PayStrategy> strategies) {
        return new PayStrategyRegistry(strategies);
    }
}
