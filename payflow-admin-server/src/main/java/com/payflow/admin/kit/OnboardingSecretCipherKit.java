package com.payflow.admin.kit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.crypto.AesEncryptor;
import com.payflow.common.crypto.CryptoProperties;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 入驻申请密钥载荷加解密（AES-256-GCM）。
 */
@Component
@RequiredArgsConstructor
public class OnboardingSecretCipherKit {

    private static final int ONBOARDING_CIPHER_ERROR = 6210;

    private final ObjectMapper objectMapper;
    private final CryptoProperties cryptoProperties;

    /**
     * 加密密钥载荷 JSON。
     */
    public String encrypt(SecretPayload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return AesEncryptor.encrypt(json, requireMasterKey());
        } catch (JsonProcessingException ex) {
            throw new BizException(ONBOARDING_CIPHER_ERROR, "密钥载荷序列化失败", ex);
        }
    }

    /**
     * 解密密钥载荷。
     */
    public SecretPayload decrypt(String cipher) {
        if (cipher == null || cipher.isBlank()) {
            throw new BizException(6209, "密钥尚未生成或已失效");
        }
        try {
            String json = AesEncryptor.decrypt(cipher, requireMasterKey());
            return objectMapper.readValue(json, SecretPayload.class);
        } catch (Exception ex) {
            throw new BizException(ONBOARDING_CIPHER_ERROR, "密钥解密失败", ex);
        }
    }

    private String requireMasterKey() {
        String key = cryptoProperties.getMasterKey();
        if (!StringUtils.hasText(key)) {
            throw new IllegalStateException("未配置 payflow.crypto.master-key，无法加解密入驻密钥");
        }
        return key.trim();
    }

    /**
     * 审批通过后暂存的明文密钥结构（加密前）。
     */
    public record SecretPayload(
            String merchantId,
            String appSecret,
            String tempPassword,
            String adminUsername,
            String loginUrl
    ) {
    }
}
