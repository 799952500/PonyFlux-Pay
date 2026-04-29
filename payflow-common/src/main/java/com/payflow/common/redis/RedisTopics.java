package com.payflow.common.redis;

/**
 * Redis Pub/Sub 频道常量。
 *
 * @author Lucas
 */
public final class RedisTopics {

    private RedisTopics() {
    }

    /**
     * 收银台配置刷新事件：当管理端修改渠道/账号/路由配置后发布，收银台订阅并刷新内存缓存。
     */
    public static final String CASHIER_CONFIG_REFRESH = "payflow:cashier:config:refresh";
}

