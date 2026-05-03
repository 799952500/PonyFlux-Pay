package com.payflow.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 调用收银台内部 API 的配置。
 *
 * @author Lucas
 */
@Data
@ConfigurationProperties(prefix = "payflow.cashier")
public class CashierClientProperties {

    private String baseUrl = "http://127.0.0.1:3002";

    private String internalToken = "";
}
