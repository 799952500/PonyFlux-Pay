package com.payflow.cashier.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.payflow.cashier.entity.Order;
import com.payflow.cashier.mapper.OrderMapper;
import com.payflow.cashier.service.OrderCacheService;
import com.payflow.cashier.service.OrderMqProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 订单超时定时任务（兜底机制）
 * 每5分钟扫描一次 PENDING/CREATED/PAYING 状态且超过30分钟未支付的订单
 * 确保 MQ 消息丢失时也有兜底处理
 *
 * @author PayFlow Team
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderMapper orderMapper;
    private final OrderCacheService orderCacheService;
    private final OrderMqProducer orderMqProducer;

    /** 超时阈值（分钟） */
    private static final int TIMEOUT_MINUTES = 30;

    /**
     * 每5分钟执行一次
     * 下一次执行 = 上一次结束时间 + interval
     */
    @Scheduled(fixedDelay = 300000, initialDelay = 60000)
    public void scanExpiredOrders() {
        log.info("[超时扫描] 开始扫描过期订单...");
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES);

            // 查询所有待支付且已过期的订单
            LambdaQueryWrapper<Order> queryWrapper = new LambdaQueryWrapper<Order>()
                    .in(Order::getStatus,
                            Order.STATUS_CREATED,
                            Order.STATUS_PAYING,
                            Order.STATUS_PENDING)
                    .lt(Order::getExpireTime, cutoff);

            var expiredOrders = orderMapper.selectList(queryWrapper);

            if (expiredOrders.isEmpty()) {
                log.info("[超时扫描] 未发现过期订单");
                return;
            }

            log.info("[超时扫描] 发现 {} 个过期订单待关单", expiredOrders.size());

            int successCount = 0;
            int failCount = 0;

            for (Order order : expiredOrders) {
                try {
                    processExpiredOrder(order);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    log.error("[超时扫描] 处理过期订单失败: orderId={}, error={}",
                            order.getOrderId(), e.getMessage());
                }
            }

            log.info("[超时扫描] 扫描完成: 成功={}, 失败={}", successCount, failCount);
        } catch (Exception e) {
            log.error("[超时扫描] 扫描任务异常: error={}", e.getMessage(), e);
        }
    }

    /**
     * 处理单个过期订单
     */
    private void processExpiredOrder(Order order) {
        String orderId = order.getOrderId();
        log.info("[超时扫描] 处理过期订单: orderId={}, createdAt={}, expireTime={}",
                orderId, order.getCreatedAt(), order.getExpireTime());

        // 1. 再次确认当前状态（防止并发）
        Order current = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>().eq(Order::getOrderId, orderId));
        if (current == null) {
            log.info("[超时扫描] 订单已不存在: orderId={}", orderId);
            return;
        }

        if (!Order.STATUS_CREATED.equals(current.getStatus())
                && !Order.STATUS_PAYING.equals(current.getStatus())
                && !Order.STATUS_PENDING.equals(current.getStatus())) {
            log.info("[超时扫描] 订单状态已变更，跳过: orderId={}, status={}",
                    orderId, current.getStatus());
            return;
        }

        // 2. 调用支付机构主动查询（模拟）
        boolean realPaid = checkPaymentWithChannel(orderId);
        if (realPaid) {
            log.info("[超时扫描] 订单实际已支付，跳过关单: orderId={}", orderId);
            return;
        }

        // 3. 执行关单
        LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<Order>()
                .eq(Order::getOrderId, orderId)
                .in(Order::getStatus,
                        Order.STATUS_CREATED,
                        Order.STATUS_PAYING,
                        Order.STATUS_PENDING)
                .set(Order::getStatus, Order.STATUS_CLOSED)
                .set(Order::getUpdatedAt, LocalDateTime.now());

        int rows = orderMapper.update(null, updateWrapper);
        if (rows > 0) {
            // 4. 清除缓存
            orderCacheService.evictOrder(orderId);
            log.info("[超时扫描] 订单已关单: orderId={}", orderId);
        } else {
            log.warn("[超时扫描] 关单更新失败（可能状态已变更）: orderId={}", orderId);
        }
    }

    /**
     * 调用支付机构主动查询订单状态（模拟）
     * TODO: 替换为真实支付渠道查询接口
     */
    private boolean checkPaymentWithChannel(String orderId) {
        try {
            log.info("[超时扫描] 主动查询支付机构: orderId={}", orderId);
            // 实际业务中：调用微信/支付宝/银联查询接口
            // 这里模拟返回未支付
            return false;
        } catch (Exception e) {
            log.warn("[超时扫描] 主动查询失败（视为未支付）: orderId={}, error={}", orderId, e.getMessage());
            return false;
        }
    }
}
