package com.payflow.recon.model;

import com.payflow.payment.core.ChannelConfigHolder;
import lombok.Builder;
import lombok.Value;

/**
 * 渠道账户视图（供 AliPayClientCache / WxPayConfigLoader 使用）。
 *
 * @author PayFlow Team
 */
@Value
@Builder
public class PayChannelAccountView implements ChannelConfigHolder {

    Long id;
    String accountCode;
    String channelConfig;

    @Override
    public String getChannelConfig() {
        return channelConfig;
    }
}
