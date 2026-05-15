package com.payflow.admin.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
/**
 * @author Lucas
 */
public class JwtProperties {

    /** JWT 签名密钥（生产环境必须通过环境变量注入，无默认值） */
    private String secret;
    private long expiration = 86400000L; // 24 hours
    private String header = "Authorization";
    private String prefix = "Bearer ";

    @PostConstruct
    public void validate() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT 密钥未配置！生产环境必须设置环境变量 JWT_SECRET。"
                + "开发环境可在 application.yml 中设置 jwt.secret。"
            );
        }
        if (secret.length() < 32) {
            log.warn("JWT 密钥长度不足 32 字符，建议使用更强的密钥以保证 HS256 安全性");
        }
    }
}
