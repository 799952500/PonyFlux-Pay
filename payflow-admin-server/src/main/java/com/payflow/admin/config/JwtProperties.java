package com.payflow.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret = "PayFlowAdminSecretKey2026VeryLongAndSecureKeyForHS256Algorithm";
    private long expiration = 86400000L; // 24 hours
    private String header = "Authorization";
    private String prefix = "Bearer ";
}
