package com.payflow.payment.union;

/**
 * 银联/云闪付支付集成入口。
 * <p>
 * 提供网关地址常量和通用工具方法。具体 API 交互由各 Handler 负责。
 * </p>
 *
 * @author PayFlow Team
 * @see UnionPayH5Handler
 * @see UnionPayQrHandler
 */
public final class UnionPayIntegration {

    /** 银联开放平台文档入口 */
    public static final String DOC_URL = "https://open.unionpay.com/";

    /** 默认银联沙箱网关 */
    public static final String SANDBOX_GATEWAY = UnionPayApiConstants.SANDBOX_GATEWAY_URL;

    /** 默认银联生产网关 */
    public static final String PROD_GATEWAY = UnionPayApiConstants.PRODUCTION_GATEWAY_URL;

    /**
     * 根据商户配置创建 HTTP 客户端。
     *
     * @param config 银联账号配置
     * @return UnionPayHttpClient 实例
     */
    public static UnionPayHttpClient createClient(UnionPayAccountConfig config) {
        return new UnionPayHttpClient(config);
    }

    private UnionPayIntegration() {
    }
}
