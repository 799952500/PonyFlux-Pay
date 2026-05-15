package com.payflow.cashier.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PayFlow 收银台自定义配置属性
 *
 * @author PayFlow Team
 */
@Slf4j
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

    /** 渠道异步通知 URL（收银台公网根地址与路径） */
    private PaymentNotify paymentNotify = new PaymentNotify();

    /** 商户签名密钥配置列表（兜底配置，优先于数据库） */
    private List<MerchantConfig> merchants = new ArrayList<>();

    /** 管理端地址（用于拉取商户支付方式路由） */
    private Admin admin = new Admin();

    /** 启动时校验关键配置项 */
    @PostConstruct
    public void validate() {
        if (jwt.secret == null || jwt.secret.isBlank()) {
            throw new IllegalStateException(
                "JWT 密钥未配置！生产环境必须设置环境变量 JWT_SECRET。"
                + "开发环境可在 application.yml 中设置 payflow.jwt.secret。"
            );
        }
        if (jwt.secret.length() < 16) {
            log.warn("JWT 密钥长度不足 16 字符，建议使用更强的密钥");
        }
        if (signature.secret == null || signature.secret.isBlank()) {
            throw new IllegalStateException(
                "签名密钥未配置！生产环境必须设置环境变量 SIGNATURE_SECRET。"
                + "开发环境可在 application.yml 中设置 payflow.signature.secret。"
            );
        }
    }

    /**
     * 根据商户号查找签名密钥配置
     *
     * @param merchantId 商户号
     * @return 商户密钥，未找到返回 null
     */
    public String getMerchantAppSecret(String merchantId) {
        return merchants.stream()
                .filter(m -> m.getId().equals(merchantId))
                .map(MerchantConfig::getAppSecret)
                .findFirst()
                .orElse(null);
    }

    @Data
    public static class Jwt {
        /** JWT 签名密钥（生产环境必须通过环境变量注入） */
        private String secret;
        /** Token 有效期（秒），默认 24 小时 */
        private long expireSeconds = 86400;
    }

    @Data
    public static class Signature {
        /** 签名密钥（生产环境必须通过环境变量注入） */
        private String secret;
        private int timestampTolerance = 300;
    }

    @Data
    public static class Cashier {
        private String baseUrl = "http://localhost:5173";
    }

    /**
     * 调用 payflow-admin-server 的配置（须与管理端 {@code payflow.cashier.internal-token} 一致）。
     */
    @Data
    public static class Admin {
        private String baseUrl = "";
        private String internalToken = "";
    }

    /**
     * 支付渠道回调 notify_url 拼接配置。
     */
    @Data
    public static class PaymentNotify {
        /** 公网根地址，不含末尾斜杠，如 https://cashier.example.com */
        private String baseUrl = "http://localhost:3002";
        /** 为 true 时微信/支付宝均使用统一回调路径（需与渠道后台配置一致） */
        private boolean useUnifiedPath = true;
        private String unifiedPath = "/api/v1/callbacks";
    }

    /**
     * 商户签名密钥配置
     */
    @Data
    public static class MerchantConfig {
        /** 商户号 */
        private String id;
        /** 商户名称 */
        private String name;
        /** 商户签名密钥 */
        private String appSecret;
    }
}
