package com.payflow.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 绑定自定义配置项。
 *
 * @author Lucas
 */
@Configuration
@EnableConfigurationProperties({CashierClientProperties.class, AdminSecurityProperties.class})
public class AdminBeansConfiguration {
}
