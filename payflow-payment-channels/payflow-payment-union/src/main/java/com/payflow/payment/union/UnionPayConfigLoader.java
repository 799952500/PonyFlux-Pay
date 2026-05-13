package com.payflow.payment.union;

import cn.hutool.json.JSONUtil;
import com.payflow.payment.core.ChannelConfigHolder;

/**
 * 从 {@link ChannelConfigHolder#getChannelConfig()} JSON 字符串中解析出
 * {@link UnionPayAccountConfig} 的静态工具。
 *
 * @author PayFlow Team
 */
public final class UnionPayConfigLoader {

    private UnionPayConfigLoader() {
    }

    /**
     * 从渠道账户的 channel_config JSON 解析配置。
     *
     * @param account 实现了 {@link ChannelConfigHolder} 的渠道账户实体
     * @return 银联配置对象
     * @throws IllegalArgumentException JSON 为空或格式错误
     */
    public static UnionPayAccountConfig load(ChannelConfigHolder account) {
        String raw = account.getChannelConfig();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("银联渠道配置(channel_config)为空");
        }
        try {
            UnionPayAccountConfig config = JSONUtil.toBean(raw, UnionPayAccountConfig.class);
            if (config.getMerId() == null || config.getMerId().isBlank()) {
                throw new IllegalArgumentException("银联商户号(merId)不能为空");
            }
            if (config.getGatewayUrl() == null || config.getGatewayUrl().isBlank()) {
                throw new IllegalArgumentException("银联网关地址(gatewayUrl)不能为空");
            }
            return config;
        } catch (Exception e) {
            throw new IllegalArgumentException("银联渠道配置解析失败: " + e.getMessage(), e);
        }
    }
}
