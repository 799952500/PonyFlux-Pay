package com.payflow.cashier.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 内部调用 API（管理端触发退款执行等）令牌配置。
 *
 * @author Lucas
 */
@Data
@ConfigurationProperties(prefix = "payflow.internal-api")
public class InternalApiProperties {

    /**
     * 与运营后台 {@code payflow.cashier.internal-token} 保持一致。
     */
    private String token = "";
}
