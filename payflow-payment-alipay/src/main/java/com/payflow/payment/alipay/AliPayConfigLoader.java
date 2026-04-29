package com.payflow.payment.alipay;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alipay.easysdk.kernel.Config;
import com.payflow.payment.core.ChannelConfigHolder;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝配置加载器。
 * <p>
 * 从 ChannelConfigHolder.channelConfig JSON 中解析出支付宝 SDK 所需的配置参数，
 * 支持正式环境和沙箱模式自动切换。
 * </p>
 *
 * <p>JSON 格式示例：</p>
 * <pre>
 * {
 *   "appId": "2021000000000000",
 *   "appSecret": "xxxxxxxxxxxxxxxx",
 *   "privateKey": "-----BEGIN RSA PRIVATE KEY-----\n...\n-----END RSA PRIVATE KEY-----",
 *   "alipayPublicKey": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
 *   "gateway": "https://openapi.alipaydev.com/gateway.do"
 * }
 * </pre>
 *
 * <p>当 appSecret 为 "SANDBOX" 时，自动启用沙箱模式。</p>
 *
 * @author PayFlow Team
 */
@Slf4j
public final class AliPayConfigLoader {

    /** 正式环境网关 */
    private static final String GATEWAY_PROD = "https://openapi.alipay.com/gateway.do";

    /** 沙箱网关 */
    private static final String GATEWAY_SANDBOX = "https://openapi.alipaydev.com/gateway.do";

    /** 沙箱标识 */
    private static final String SANDBOX = "SANDBOX";

    private AliPayConfigLoader() {
        // 工具类，禁止实例化
    }

    /**
     * 从渠道账户配置中加载并构建支付宝 SDK Config。
     *
     * @param account 渠道账户实体，channelConfig 字段需包含有效的 JSON 配置
     * @return 支付宝 SDK Config 对象，可直接用于 AlipayEaseSdk.Client
     * @throws IllegalArgumentException 如果配置缺失或格式错误
     */
    public static Config load(ChannelConfigHolder account) {
        if (account == null || StrUtil.isBlank(account.getChannelConfig())) {
            throw new IllegalArgumentException("渠道账户配置为空");
        }

        JSONObject json = JSONUtil.parseObj(account.getChannelConfig());

        String appId = json.getStr("appId");
        String appSecret = json.getStr("appSecret");
        String privateKey = json.getStr("privateKey");
        String alipayPublicKey = json.getStr("alipayPublicKey");
        String gateway = json.getStr("gateway");

        if (StrUtil.isBlank(appId)) {
            throw new IllegalArgumentException("支付宝配置缺少 appId");
        }
        if (StrUtil.isBlank(privateKey)) {
            throw new IllegalArgumentException("支付宝配置缺少 privateKey");
        }
        if (StrUtil.isBlank(alipayPublicKey)) {
            throw new IllegalArgumentException("支付宝配置缺少 alipayPublicKey");
        }

        // 判断是否为沙箱模式
        boolean sandbox = SANDBOX.equalsIgnoreCase(appSecret);
        if (StrUtil.isBlank(gateway)) {
            gateway = sandbox ? GATEWAY_SANDBOX : GATEWAY_PROD;
        }

        log.info("加载支付宝配置: appId={}, gateway={}, sandbox={}", appId, gateway, sandbox);

        Config config = new Config();
        config.protocol = "https";
        config.gatewayHost = extractHost(gateway);
        config.signType = "RSA2";
        config.appId = appId;
        config.merchantPrivateKey = privateKey;
        config.alipayPublicKey = alipayPublicKey;
        // optional: soapService 可不设置

        return config;
    }

    /**
     * 从完整 gateway URL 中提取 host 部分。
     *
     * @param gateway 完整网关 URL
     * @return host:port 部分
     */
    private static String extractHost(String gateway) {
        if (gateway == null) {
            return "openapi.alipay.com";
        }
        String trimmed = gateway.trim();
        if (trimmed.startsWith("https://")) {
            trimmed = trimmed.substring(8);
        } else if (trimmed.startsWith("http://")) {
            trimmed = trimmed.substring(7);
        }
        int slashIdx = trimmed.indexOf('/');
        if (slashIdx > 0) {
            return trimmed.substring(0, slashIdx);
        }
        return trimmed;
    }
}
