package com.payflow.cashier.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PayFlow 收银台自定义配置属性
 *
 * @author PayFlow Team
 */
@Data
@Component
@ConfigurationProperties(prefix = "payflow")
public class PayflowProperties {

    /** JWT 配置 */
    private Jwt jwt = new Jwt();

    /** 签名配置 */
    private Signature signature = new Signature();

    /** 收银台配置 */
    private Cashier cashier = new Cashier();

    @Data
    public static class Jwt {
        /** JWT 签名密钥 */
        private String secret = "default_jwt_secret";
        /** Token 有效期（秒），默认 24 小时 */
        private long expireSeconds = 86400;
    }

    @Data
    public static class Signature {
        private String secret = "default_signature_secret";
        private int timestampTolerance = 300;
    }

    @Data
    public static class Cashier {
        private String baseUrl = "http://localhost:5173";
    }
}
