package com.payflow.recon.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * xxl-job 执行器（无调度中心时保持 xxl.job.enabled=false）。
 *
 * @author PayFlow Team
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "xxl.job.enabled", havingValue = "true")
public class XxlJobConfig {

    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor(
            @Value("${xxl.job.admin.addresses}") String adminAddresses,
            @Value("${xxl.job.executor.appname}") String appname,
            @Value("${xxl.job.executor.port}") int port,
            @Value("${xxl.job.executor.logpath:./logs/xxl-job}") String logPath,
            @Value("${xxl.job.executor.logretentiondays:7}") int logRetentionDays) {
        log.info("注册 xxl-job 执行器: appname={}, port={}", appname, port);
        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
        executor.setAdminAddresses(adminAddresses);
        executor.setAppname(appname);
        executor.setPort(port);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);
        return executor;
    }
}
