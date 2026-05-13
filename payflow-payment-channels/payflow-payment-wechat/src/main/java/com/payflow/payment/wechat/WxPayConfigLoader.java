package com.payflow.payment.wechat;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.payflow.payment.core.ChannelConfigHolder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.util.Base64;

/**
 * 微信支付配置加载器。
 * <p>
 * 从 ChannelConfigHolder.channelConfig JSON 中解析出微信支付 v3 API 所需的配置参数，
 * 包括商户号（mchId）、应用ID（appId）、APIv3密钥（mchKey）和商户私钥（证书）。
 * </p>
 *
 * <p>JSON 格式示例：</p>
 * <pre>
 * {
 *   "appId": "wx1234567890abcdef",
 *   "mchId": "1234567890",
 *   "mchKey": "32位APIv3密钥",
 *   "privateKey": "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
 *   "privateKeyPath": "D:/certs/apiclient_key.pem",
 *   "certSerialNo": "序列号",
 *   "certPath": "D:/certs/apiclient_cert.p12",
 *   "certPassword": "证书密码"
 * }
 * </pre>
 *
 * @author PayFlow Team
 */
@Slf4j
public final class WxPayConfigLoader {

    private WxPayConfigLoader() {
    }

    /**
     * 从渠道账户配置中加载微信支付参数。
     *
     * @param account 渠道账户实体
     * @return WxPayAccountConfig 配置对象
     */
    public static WxPayAccountConfig load(ChannelConfigHolder account) {
        if (account == null || StrUtil.isBlank(account.getChannelConfig())) {
            throw new IllegalArgumentException("渠道账户配置为空");
        }

        JSONObject json = JSONUtil.parseObj(account.getChannelConfig());

        String appId = json.getStr("appId");
        String mchId = json.getStr("mchId");
        String mchKey = json.getStr("mchKey");
        String privateKey = json.getStr("privateKey");
        String privateKeyPath = json.getStr("privateKeyPath");
        String certSerialNo = json.getStr("certSerialNo");
        String certPath = json.getStr("certPath");
        String certPassword = json.getStr("certPassword");

        if (StrUtil.isBlank(appId)) {
            throw new IllegalArgumentException("微信支付配置缺少 appId");
        }
        if (StrUtil.isBlank(mchId)) {
            throw new IllegalArgumentException("微信支付配置缺少 mchId");
        }
        if (StrUtil.isBlank(mchKey)) {
            throw new IllegalArgumentException("微信支付配置缺少 mchKey");
        }
        if (StrUtil.isBlank(privateKey) && StrUtil.isBlank(privateKeyPath)) {
            throw new IllegalArgumentException("微信支付配置缺少 privateKey 或 privateKeyPath");
        }

        WxPayAccountConfig config = new WxPayAccountConfig();
        config.appId = appId;
        config.mchId = mchId;
        config.mchKey = mchKey;

        // 优先使用 PEM 格式私钥字符串
        if (StrUtil.isNotBlank(privateKey)) {
            config.privateKey = privateKey;
        }

        // 证书序列号
        if (StrUtil.isNotBlank(certSerialNo)) {
            config.certSerialNo = certSerialNo;
        }

        // P12 证书路径（退款等需要证书的操作）
        if (StrUtil.isNotBlank(certPath)) {
            config.certPath = certPath;
            config.certPassword = StrUtil.isBlank(certPassword) ? mchId : certPassword;
        }

        log.info("加载微信支付配置: appId={}, mchId={}", appId, mchId);
        return config;
    }

    /**
     * 获取商户证书的 SSLContext（用于退款等需要双向证书的操作）。
     *
     * @param config 微信支付配置
     * @return SSLContext
     */
    public static SSLContext getCertSSLContext(WxPayAccountConfig config) {
        if (StrUtil.isBlank(config.certPath)) {
            log.warn("未配置证书路径，无法创建 SSLContext");
            return null;
        }

        try {
            char[] password = config.certPassword.toCharArray();
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            File certFile = new File(config.certPath);
            if (!certFile.exists()) {
                log.error("证书文件不存在: {}", config.certPath);
                return null;
            }
            try (FileInputStream fis = new FileInputStream(certFile)) {
                keyStore.load(fis, password);
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, password);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            return sslContext;
        } catch (Exception e) {
            log.error("加载商户证书失败: {}", config.certPath, e);
            return null;
        }
    }
}
