package com.payflow.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 调用对账服务内部 API 的配置。
 *
 * @author PayFlow Team
 */
@Data
@ConfigurationProperties(prefix = "payflow.recon")
public class ReconClientProperties {

    /** 对账服务根地址，如 http://127.0.0.1:3004 */
    private String baseUrl = "http://127.0.0.1:3004";

    /** 与对账服务 {@code InternalApiTokenInterceptor} 请求头一致 */
    private String internalToken = "";
}
