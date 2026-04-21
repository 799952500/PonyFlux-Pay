package com.payflow.admin.mapper.cashier;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.cashier.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单 Mapper（cashier 库）
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 按商户查询订单
     */
    @Select("SELECT * FROM orders WHERE merchant_id = #{merchantId} ORDER BY created_at DESC")
    List<Order> findByMerchantId(@Param("merchantId") String merchantId);

    /**
     * 按状态查询订单
     */
    @Select("SELECT * FROM orders WHERE status = #{status} ORDER BY created_at DESC")
    List<Order> findByStatus(@Param("status") String status);

    /**
     * 按时间范围查询订单
     */
    @Select("SELECT * FROM orders WHERE created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at DESC")
    List<Order> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各状态订单数量
     */
    @Select("SELECT status, COUNT(*) as count FROM orders GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus();
}
