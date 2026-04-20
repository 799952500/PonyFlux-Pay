package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.service.OrderCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 订单缓存服务 NoOp 实现（无 Redis 环境降级用）
 * 使用 Spring Cache 标准接口，当 Redis 不可用时降级到直接查 DB
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "payflow.cache.redis.enabled", havingValue = "false", matchIfMissing = true)
public class OrderCacheNoOpServiceImpl implements OrderCacheService {

    private final OrderMapper orderMapper;

    @Override
    public void cacheOrder(String orderId, Order order) {
        log.debug("[NoOp-Cache] 跳过缓存: orderId={}", orderId);
    }

    @Override
    public Order getOrder(String orderId) {
        log.debug("[NoOp-Cache] 跳过缓存查询: orderId={}", orderId);
        return null;
    }

    @Override
    public void evictOrder(String orderId) {
        log.debug("[NoOp-Cache] 跳过缓存清除: orderId={}", orderId);
    }

    @Override
    public boolean exists(String orderId) {
        return false;
    }

    @Override
    public void updateCache(String orderId, Order order) {
        log.debug("[NoOp-Cache] 跳过缓存更新: orderId={}", orderId);
    }

    @Override
    public Order getOrderWithFallback(String orderId) {
        log.debug("[NoOp-Cache] 直接查询DB: orderId={}", orderId);
        try {
            return orderMapper.selectOne(
                    new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
        } catch (Exception e) {
            log.warn("DB查询异常: orderId={}, error={}", orderId, e.getMessage());
            return null;
        }
    }
}
