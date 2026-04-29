package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
                              LocalDateTime startTime, LocalDateTime endTime) {
        Page<Order> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        
        if (merchantId != null && !merchantId.isEmpty()) {
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
     * 统计各状态订单数量
     */
    public List<Map<String, Object>> countByStatus() {
        return orderMapper.countByStatus();
    }

    /**
     * 获取订单总数
     */
    public long count() {
        return orderMapper.selectCount(null);
    }
}
