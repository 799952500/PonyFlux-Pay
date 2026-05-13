package com.payflow.payment.alipay;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import com.payflow.payment.core.ChannelConfigHolder;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付宝 SDK 配置工具类。
 * <p>
 * 负责将渠道账户配置转换为 Alipay Config，
 * 并在调用前初始化 Factory。
 * </p>
 *
 * @author PayFlow Team
 */
@Slf4j
public final class AliPayClientCache {

    private AliPayClientCache() {
    }

    /**
     * 使用指定账户配置初始化 Alipay SDK 工厂。
     * <p>
     * 必须在调用 Factory 静态方法之前调用。
     * </p>
     *
     * @param account 渠道账户
     */
    public static void configure(ChannelConfigHolder account) {
        Config config = AliPayConfigLoader.load(account);
        Factory.setOptions(config);
    }
}
