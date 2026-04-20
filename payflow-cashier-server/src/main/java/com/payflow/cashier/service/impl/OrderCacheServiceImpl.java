package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.config.CacheConfig;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.service.OrderCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * 订单缓存服务实现（Spring Cache 注解方式）
 * - 写操作：先操作DB，成功后更新缓存
 * - 查询时：先查缓存，未命中再查DB并回填缓存
 * - TTL：30分钟（在 CacheConfig 中配置）
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "payflow.cache.redis.enabled", havingValue = "true")
public class OrderCacheServiceImpl implements OrderCacheService {

    private final OrderMapper orderMapper;

    @Autowired(required = false)
    private CacheManager cacheManager;

    public OrderCacheServiceImpl(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    /**
     * 缓存订单（内部使用 @CachePut 注解）
     * 注意：此方法被 @CachePut 标记后，Spring 会自动处理缓存写入
     */
    @Override
    @CachePut(cacheNames = CacheConfig.CACHE_NAME_ORDER, key = "#orderId")
    public void cacheOrder(String orderId, Order order) {
        log.debug("缓存订单: orderId={}", orderId);
    }

    /**
     * 查询缓存
     * 使用 @Cacheable 注解，Spring 会自动拦截并从缓存获取
     * 只有缓存未命中时才会执行方法体
     */
    @Override
    @Cacheable(cacheNames = CacheConfig.CACHE_NAME_ORDER, key = "#orderId", unless = "#result == null")
    public Order getOrder(String orderId) {
        // 只有缓存未命中时才会执行到这里
        // 但 Cache-Aside 模式的回填逻辑在 getOrderWithFallback 中实现
        log.debug("缓存未命中: orderId={}", orderId);
        return null;
    }

    /**
     * 先查缓存，未命中则查DB并回填缓存（用于对外查询接口）
     * 手动实现 Cache-Aside 模式
     */
    @Override
    public Order getOrderWithFallback(String orderId) {
        if (orderId == null) {
            return null;
        }
        // 1. 先查缓存（手动从缓存获取）
        Order cached = getOrderFromCache(orderId);
        if (cached != null) {
            log.debug("缓存命中: orderId={}", orderId);
            return cached;
        }
        // 2. 缓存未命中，查 DB
        log.debug("缓存未命中，回填DB: orderId={}", orderId);
        try {
            Order order = orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
            if (order != null) {
                // 回填缓存
                cacheOrder(orderId, order);
            }
            return order;
        } catch (Exception e) {
            log.warn("DB查询异常: orderId={}, error={}", orderId, e.getMessage());
            return null;
        }
    }

    /**
     * 从缓存获取（内部方法）
     */
    private Order getOrderFromCache(String orderId) {
        Cache cache = getCache();
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(orderId);
            if (wrapper != null) {
                return (Order) wrapper.get();
            }
        }
        return null;
    }

    /**
     * 获取缓存实例
     */
    private Cache getCache() {
        if (cacheManager == null) {
            log.warn("CacheManager 未配置");
            return null;
        }
        return cacheManager.getCache(CacheConfig.CACHE_NAME_ORDER);
    }

    /**
     * 删除缓存
     * 使用 @CacheEvict 注解删除缓存
     */
    @Override
    @CacheEvict(cacheNames = CacheConfig.CACHE_NAME_ORDER, key = "#orderId")
    public void evictOrder(String orderId) {
        log.debug("删除缓存: orderId={}", orderId);
    }

    /**
     * 判断缓存是否存在
     */
    @Override
    public boolean exists(String orderId) {
        if (orderId == null) {
            return false;
        }
        return getOrderFromCache(orderId) != null;
    }

    /**
     * 更新缓存
     * 使用 @CachePut 注解更新缓存
     */
    @Override
    @CachePut(cacheNames = CacheConfig.CACHE_NAME_ORDER, key = "#orderId")
    public void updateCache(String orderId, Order order) {
        log.debug("更新缓存: orderId={}", orderId);
    }
}