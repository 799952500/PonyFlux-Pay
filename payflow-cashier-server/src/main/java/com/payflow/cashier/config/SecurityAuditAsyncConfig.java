package com.payflow.cashier.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 安全审计异步写入线程池。
 *
 * @author PayFlow Team
 */
@Configuration
public class SecurityAuditAsyncConfig {

    @Bean(name = "securityAuditExecutor")
    public Executor securityAuditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("security-audit-");
        executor.initialize();
        return executor;
    }
}
