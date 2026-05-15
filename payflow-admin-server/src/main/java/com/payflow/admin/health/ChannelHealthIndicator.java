package com.payflow.admin.health;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.PaymentAccount;
import com.payflow.admin.service.PaymentAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 支付渠道健康指示器——检查各渠道账户是否可用。
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelHealthIndicator implements HealthIndicator {

    private final PaymentAccountService paymentAccountService;

    @Override
    public Health health() {
        try {
            List<PaymentAccount> accounts = paymentAccountService.listAll();
            long enabledCount = accounts.stream()
                    .filter(a -> a.getEnabled() != null && a.getEnabled())
                    .count();
            long totalCount = accounts.size();

            Health.Builder builder = enabledCount > 0 ? Health.up() : Health.down();
            return builder
                    .withDetail("totalAccounts", totalCount)
                    .withDetail("enabledAccounts", enabledCount)
                    .withDetail("disabledAccounts", totalCount - enabledCount)
                    .build();
        } catch (Exception e) {
            log.error("渠道健康检查异常", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
