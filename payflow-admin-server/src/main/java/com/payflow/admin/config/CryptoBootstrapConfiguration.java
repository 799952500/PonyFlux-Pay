package com.payflow.admin.config;

import com.payflow.common.crypto.AesEncryptor;
import com.payflow.common.crypto.CryptoProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * 启动时注入 AES 主密钥，供 EncryptedStringTypeHandler 与入驻密钥加解密使用。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoBootstrapConfiguration {

    private final CryptoProperties cryptoProperties;

    @PostConstruct
    void initMasterKey() {
        String key = cryptoProperties.getMasterKey();
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException("请配置 payflow.crypto.master-key（生产环境通过 MASTER_KEY 注入）");
        }
        String trimmed = key.trim();
        try {
            AesEncryptor.validateMasterKey(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "payflow.crypto.master-key / MASTER_KEY 无效：须为 Base64 编码的 32 字节随机数（可用 openssl rand -base64 32 生成）。"
                            + " " + ex.getMessage(), ex);
        }
        EncryptedStringTypeHandler.setMasterKey(trimmed);
        log.info("AES 主密钥已加载（入驻密钥与敏感字段加解密）");
    }
}
