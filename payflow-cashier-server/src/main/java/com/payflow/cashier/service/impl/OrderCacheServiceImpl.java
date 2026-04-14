package com.payflow.cashier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.service.OrderCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 订单缓存服务实现（Cache-Aside 模式）
 * - 写操作：先操作DB，成功后更新缓存
 * - 查询时：先查缓存，未命中再查DB并回填缓存
 * - TTL：30分钟
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCacheServiceImpl implements OrderCacheService {

    private static final String ORDER_KEY_PREFIX = "order:";
    private static final long CACHE_TTL_MINUTES = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderMapper orderMapper;

    @Override
    public void cacheOrder(String orderId, Order order) {
        if (orderId == null || order == null) {
            return;
        }
        try {
            String key = buildKey(orderId);
            redisTemplate.opsForValue().set(key, order, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("缓存订单成功: orderId={}", orderId);
        } catch (Exception e) {
            log.warn("缓存订单失败（不影响主业务）: orderId={}, error={}", orderId, e.getMessage());
        }
    }

    @Override
    public Order getOrder(String orderId) {
        if (orderId == null) {
            return null;
        }
        try {
            String key = buildKey(orderId);
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                log.debug("缓存命中: orderId={}", orderId);
                return (Order) cached;
            }
            log.debug("缓存未命中，回填DB: orderId={}", orderId);
            return null;
        } catch (Exception e) {
            log.warn("查询缓存异常（降级走DB）: orderId={}, error={}", orderId, e.getMessage());
            return null;
        }
    }

    /**
     * 先查缓存，未命中则查DB并回填缓存（用于对外查询接口）
     * 注意：本类已是 public，不需要额外 public 声明
     */
    public Order getOrderWithFallback(String orderId) {
        // 1. 先查缓存
        Order cached = getOrder(orderId);
        if (cached != null) {
            return cached;
        }
        // 2. 缓存未命中，查DB
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

    @Override
    public void evictOrder(String orderId) {
        if (orderId == null) {
            return;
        }
        try {
            String key = buildKey(orderId);
            redisTemplate.delete(key);
            log.debug("删除缓存成功: orderId={}", orderId);
        } catch (Exception e) {
            log.warn("删除缓存失败（不影响主业务）: orderId={}, error={}", orderId, e.getMessage());
        }
    }

    @Override
    public boolean exists(String orderId) {
        if (orderId == null) {
            return false;
        }
        try {
            String key = buildKey(orderId);
            Boolean result = redisTemplate.hasKey(key);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.warn("判断缓存是否存在异常: orderId={}, error={}", orderId, e.getMessage());
            return false;
        }
    }

    @Override
    public void updateCache(String orderId, Order order) {
        // 更新时直接覆盖，等同于 cacheOrder
        cacheOrder(orderId, order);
    }

    private String buildKey(String orderId) {
        return ORDER_KEY_PREFIX + orderId;
    }
}
