package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.mapper.cashier.OrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单服务（查询 cashier 库）
  * @author Lucas
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;

    /**
     * 分页查询订单
     */
    public IPage<Order> page(int pageNum, int pageSize, String merchantId, String status,
                              LocalDateTime startTime, LocalDateTime endTime,
                              java.util.List<String> merchantScopeIds) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        if (merchantScopeIds != null && merchantScopeIds.isEmpty()) {
            page.setTotal(0);
            return page;
        }
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();

        if (merchantScopeIds != null && !merchantScopeIds.isEmpty()) {
            if (merchantId != null && !merchantId.isEmpty()) {
                if (!merchantScopeIds.contains(merchantId)) {
                    page.setTotal(0);
                    return page;
                }
                wrapper.eq(Order::getMerchantId, merchantId);
            } else {
                wrapper.in(Order::getMerchantId, merchantScopeIds);
            }
        } else if (merchantId != null && !merchantId.isEmpty()) {
            wrapper.eq(Order::getMerchantId, merchantId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        if (startTime != null) {
            wrapper.ge(Order::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(Order::getCreatedAt, endTime);
        }
        
        wrapper.orderByDesc(Order::getCreatedAt);
        return orderMapper.selectPage(page, wrapper);
    }

    /**
     * 根据订单号查询
     */
    public Order getByOrderId(String orderId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getOrderId, orderId);
        return orderMapper.selectOne(wrapper);
    }

    /**
     * 批量按订单号查询（回调列表等场景避免 N+1）。
     */
    public Map<String, Order> mapByOrderIds(List<String> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return Map.of();
        }
        List<String> distinct = orderIds.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Order::getOrderId, distinct);
        return orderMapper.selectList(wrapper).stream()
                .collect(java.util.stream.Collectors.toMap(Order::getOrderId, o -> o, (a, b) -> a));
    }

    /**
     * 按商户查询订单
     */
    public List<Order> findByMerchantId(String merchantId) {
        return orderMapper.findByMerchantId(merchantId);
    }

    /**
     * 按状态查询订单
     */
    public List<Order> findByStatus(String status) {
        return orderMapper.findByStatus(status);
    }

    /**
     * 按时间范围查询订单
     */
    public List<Order> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return orderMapper.findByTimeRange(startTime, endTime);
    }

    /**
     * 统计各状态订单数量（与列表查询共用商户范围筛选）
     */
    public List<Map<String, Object>> countByStatus(String merchantId, List<String> merchantScopeIds) {
        if (merchantScopeIds != null && merchantScopeIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.select("status", "COUNT(*) AS cnt");
        wrapper.groupBy("status");
        applyMerchantScope(wrapper, merchantId, merchantScopeIds);
        return orderMapper.selectMaps(wrapper);
    }

    /**
     * 获取订单总数（与列表查询共用商户范围筛选）
     */
    public long count(String merchantId, List<String> merchantScopeIds) {
        if (merchantScopeIds != null && merchantScopeIds.isEmpty()) {
            return 0L;
        }
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (merchantScopeIds != null && !merchantScopeIds.isEmpty()) {
            if (merchantId != null && !merchantId.isEmpty()) {
                if (!merchantScopeIds.contains(merchantId)) {
                    return 0L;
                }
                wrapper.eq(Order::getMerchantId, merchantId);
            } else {
                wrapper.in(Order::getMerchantId, merchantScopeIds);
            }
        } else if (merchantId != null && !merchantId.isEmpty()) {
            wrapper.eq(Order::getMerchantId, merchantId);
        }
        return orderMapper.selectCount(wrapper);
    }

    private static void applyMerchantScope(QueryWrapper<Order> wrapper, String merchantId,
                                           List<String> merchantScopeIds) {
        if (merchantScopeIds != null && !merchantScopeIds.isEmpty()) {
            if (merchantId != null && !merchantId.isEmpty()) {
                if (!merchantScopeIds.contains(merchantId)) {
                    wrapper.apply("1 = 0");
                    return;
                }
                wrapper.eq("merchant_id", merchantId);
            } else {
                wrapper.in("merchant_id", merchantScopeIds);
            }
        } else if (merchantId != null && !merchantId.isEmpty()) {
            wrapper.eq("merchant_id", merchantId);
        }
    }

    /**
     * 导出用：按与分页列表相同的筛选条件取最多 maxRows 条。
     */
    public List<Order> listForExport(int maxRows, String merchantId, String status,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     List<String> merchantScopeIds) {
        int cap = Math.min(Math.max(maxRows, 1), 5000);
        if (merchantScopeIds != null && merchantScopeIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (merchantScopeIds != null && !merchantScopeIds.isEmpty()) {
            if (merchantId != null && !merchantId.isEmpty()) {
                if (!merchantScopeIds.contains(merchantId)) {
                    return List.of();
                }
                wrapper.eq(Order::getMerchantId, merchantId);
            } else {
                wrapper.in(Order::getMerchantId, merchantScopeIds);
            }
        } else if (merchantId != null && !merchantId.isEmpty()) {
            wrapper.eq(Order::getMerchantId, merchantId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Order::getStatus, status);
        }
        if (startTime != null) {
            wrapper.ge(Order::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(Order::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(Order::getCreatedAt).last("LIMIT " + cap);
        return orderMapper.selectList(wrapper);
    }

    /**
     * 获取商户交易洞察：近30天趋势、渠道偏好、退款率、最后交易时间。
     */
    public Map<String, Object> getMerchantInsight(String merchantId) {
        java.util.Map<String, Object> insight = new java.util.LinkedHashMap<>();

        // 近30天每日交易趋势
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate start = today.minusDays(30);
        java.util.List<java.util.Map<String, Object>> trendRows = orderMapper.merchantTrend30Days(merchantId, start);
        insight.put("trend30Days", trendRows);

        // 渠道偏好分布
        java.util.List<java.util.Map<String, Object>> channelPrefs = orderMapper.merchantChannelPrefs(merchantId, start);
        insight.put("channelPreferences", channelPrefs);

        // 退款率
        java.util.Map<String, Object> refundRate = orderMapper.merchantRefundRate(merchantId, start);
        insight.put("refundRate", refundRate != null ? refundRate : java.util.Map.of("refundCount", 0L, "totalCount", 0L, "rate", "0%"));

        // 最后交易时间
        Order lastOrder = orderMapper.selectOne(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getMerchantId, merchantId)
                        .orderByDesc(Order::getCreatedAt)
                        .last("LIMIT 1"));
        insight.put("lastTradeTime", lastOrder != null ? lastOrder.getCreatedAt().toString() : null);
        insight.put("merchantId", merchantId);

        return insight;
    }
}
