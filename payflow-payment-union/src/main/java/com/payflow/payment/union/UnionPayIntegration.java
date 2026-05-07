package com.payflow.payment.union;

/**
 * 银联在线 / 云闪付 H5 对接占位：实际生产需接入银联开放平台网关。
 * 参考文档：https://open.unionpay.com/
 */
public final class UnionPayIntegration {

    /**
     * 占位跳转页（收银台可引导用户了解接入方式），非真实支付网关。
     */
    public static final String PLACEHOLDER_H5_URL = "https://open.unionpay.com/";

    private UnionPayIntegration() {
    }
}
