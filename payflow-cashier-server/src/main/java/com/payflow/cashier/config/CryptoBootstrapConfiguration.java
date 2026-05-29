package com.payflow.cashier.config;

import com.payflow.common.crypto.CryptoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * 启动时注入 AES 主密钥，供收银台 channelConfig 等敏感字段加解密。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(CryptoProperties.class)
public class CryptoBootstrapConfiguration {

    private final CryptoProperties cryptoProperties;

    @PostConstruct
    public void initMasterKey() {
        String key = cryptoProperties.getMasterKey();
        if (key == null || key.isBlank()) {
            log.warn("payflow.crypto.master-key 未配置，EncryptedStringTypeHandler 不可用");
            return;
        }
        EncryptedStringTypeHandler.setMasterKey(key.trim());
    }
}
