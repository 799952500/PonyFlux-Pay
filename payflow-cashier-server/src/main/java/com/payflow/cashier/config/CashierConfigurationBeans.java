package com.payflow.cashier.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 启用收银台配置属性绑定。
 *
 * @author Lucas
 */
@Configuration
@EnableConfigurationProperties(InternalApiProperties.class)
public class CashierConfigurationBeans {
}
