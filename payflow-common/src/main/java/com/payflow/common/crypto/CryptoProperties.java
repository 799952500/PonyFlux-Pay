package com.payflow.common.crypto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 加密配置属性。
 *
 * <p>在 application.yml 中配置：</p>
 * <pre>
 * payflow:
 *   crypto:
 *     master-key: "Base64编码的256位AES密钥"
 * </pre>
 *
 * <p>生成新密钥：{@code AesEncryptor.generateMasterKey()}</p>
 *
 * @author Lucas
 */
@Data
@ConfigurationProperties(prefix = "payflow.crypto")
public class CryptoProperties {

    /**
     * AES-256 主密钥（Base64 编码，32 字节 = 256 位）。
     *
     * <p>用于加密/解密商户密钥、证书密码等敏感数据。</p>
     */
    private String masterKey;
}
