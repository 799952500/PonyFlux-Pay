package com.payflow.cashier.service;

import com.payflow.cashier.entity.Order;

/**
 * 订单缓存服务接口
 * 提供 Redis 缓存操作，Cache-Aside 模式
 *
 * 使用 Spring Cache 注解方式
 *
 * @author PayFlow Team
 */
public interface OrderCacheService {

    /**
     * 缓存订单（TTL=30分钟）
     *
     * @param orderId 订单号
     * @param order 订单实体
     */
    void cacheOrder(String orderId, Order order);

    /**
     * 查询缓存（优先）
     *
     * @param orderId 订单号
     * @return 订单实体，未命中返回 null
     */
    Order getOrder(String orderId);

    /**
     * 删除缓存（DB写成功后同步删除）
     *
     * @param orderId 订单号
     */
    void evictOrder(String orderId);

    /**
     * 判断缓存是否存在
     *
     * @param orderId 订单号
     * @return 是否存在
     */
    boolean exists(String orderId);

    /**
     * 更新缓存（DB更新后同步更新缓存）
     *
     * @param orderId 订单号
     * @param order 订单实体
     */
    void updateCache(String orderId, Order order);

    /**
     * 先查缓存，未命中则查DB并回填缓存
     * 用于对外查询接口（商户查询、收银台查询）
     *
     * @param orderId 订单号
     * @return 订单实体，未命中返回 null
     */
    Order getOrderWithFallback(String orderId);
}