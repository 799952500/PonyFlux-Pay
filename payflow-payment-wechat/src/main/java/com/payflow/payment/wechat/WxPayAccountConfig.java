package com.payflow.payment.wechat;

import lombok.Data;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * 微信支付账户配置。
 * <p>
 * 由 WxPayConfigLoader 从 JSON 配置中解析生成。
 * </p>
 *
 * @author PayFlow Team
 */
@Data
public class WxPayAccountConfig {

    /** 微信公众号/小程序/APP的应用ID */
    public String appId;

    /** 商户号 */
    public String mchId;

    /** APIv3 密钥（32位） */
    public String mchKey;

    /** 商户私钥（PEM 格式字符串） */
    public String privateKey;

    /** 解析后的 PrivateKey 对象（懒加载） */
    private PrivateKey privateKeyObj;

    /** 证书序列号 */
    public String certSerialNo;

    /** P12 证书文件路径（退款等敏感操作需要） */
    public String certPath;

    /** P12 证书密码，默认等于 mchId */
    public String certPassword;

    /**
     * 获取解析后的商户私钥（RSA）。
     * <p>
     * 首次调用时从 PEM 字符串解析并缓存。
     * </p>
     *
     * @return PrivateKey 对象
     * @throws IllegalStateException 如果私钥格式错误
     */
    public PrivateKey getPrivateKeyObj() {
        if (privateKeyObj != null) {
            return privateKeyObj;
        }
        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException("私钥为空，无法进行签名");
        }
        try {
            // 去除 PEM 头尾和换行
            String pem = privateKey
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            privateKeyObj = kf.generatePrivate(keySpec);
            return privateKeyObj;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("解析商户私钥失败", e);
        }
    }
}
