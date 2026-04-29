package com.payflow.payment.core;

/**
 * 渠道账户中与 SDK 相关的最小契约（避免支付 Jar 依赖收银台实体）。
  * @author Lucas
 */
public interface ChannelConfigHolder {

    /**
     * @return 渠道 JSON 配置（appId、mchId、证书等）
     */
    String getChannelConfig();
}
