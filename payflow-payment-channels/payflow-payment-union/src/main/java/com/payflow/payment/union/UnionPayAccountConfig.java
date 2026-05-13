package com.payflow.payment.union;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 银联支付账号配置 POJO —— 从 {@code channel_config} JSON 解析得到。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnionPayAccountConfig {

    /** 银联商户号 */
    private String merId;

    /** 签名证书文件路径（.pfx / PKCS12） */
    private String signCertPath;

    /** 签名证书密码（已通过 AES-256-GCM 加密存储） */
    private String signCertPassword;

    /** 加密证书文件路径（.cer，可选） */
    private String encryptCertPath;

    /** 加密证书密码（可选） */
    private String encryptCertPassword;

    /** 银联网关地址（沙箱或生产） */
    private String gatewayUrl;

    /** 自定义回调地址（为空则使用系统默认） */
    private String notifyUrl;

    /** 银联公钥证书路径（验签用，.cer） */
    private String unionPublicKeyPath;
}
