package com.payflow.admin.mapper.cashier;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.cashier.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper（cashier 库）
  * @author Lucas
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * 按商户查询订单
     */
    @Select("SELECT * FROM cashier_orders WHERE merchant_id = #{merchantId} ORDER BY created_at DESC")
    List<Order> findByMerchantId(@Param("merchantId") String merchantId);

    /**
     * 按状态查询订单
     */
    @Select("SELECT * FROM cashier_orders WHERE status = #{status} ORDER BY created_at DESC")
    List<Order> findByStatus(@Param("status") String status);

    /**
     * 按时间范围查询订单
     */
    @Select("SELECT * FROM cashier_orders WHERE created_at BETWEEN #{startTime} AND #{endTime} ORDER BY created_at DESC")
    List<Order> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各状态订单数量
     */
    @Select("SELECT status, COUNT(*) as count FROM cashier_orders GROUP BY status")
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 指定日「已支付」订单实付金额合计（分）
     */
    @Select("SELECT COALESCE(SUM(pay_amount), 0) FROM cashier_orders WHERE status IN ('PAID', 'SUCCESS') "
            + "AND DATE(COALESCE(pay_time, updated_at, created_at)) = #{day}")
    Long sumPaidRevenueFenOnDay(@Param("day") LocalDate day);

    /**
     * 指定日创建订单数
     */
    @Select("SELECT COUNT(*) FROM cashier_orders WHERE DATE(created_at) = #{day}")
    Long countCreatedOnDay(@Param("day") LocalDate day);

    /**
     * 指定日已支付订单数
     */
    @Select("SELECT COUNT(*) FROM cashier_orders WHERE status IN ('PAID', 'SUCCESS') "
            + "AND DATE(COALESCE(pay_time, updated_at, created_at)) = #{day}")
    Long countPaidOnDay(@Param("day") LocalDate day);

    /**
     * 近 N 天按日聚合（含当日），用于趋势图
     */
    @Select("SELECT DATE(created_at) AS dayBucket, COUNT(*) AS orders, "
            + "COALESCE(SUM(CASE WHEN status IN ('PAID', 'SUCCESS') THEN COALESCE(pay_amount, amount) ELSE 0 END), 0) AS revenue, "
            + "SUM(CASE WHEN status IN ('PAID', 'SUCCESS') THEN 1 ELSE 0 END) AS paid "
            + "FROM cashier_orders WHERE DATE(created_at) >= #{start} "
            + "GROUP BY DATE(created_at) ORDER BY dayBucket")
    List<Map<String, Object>> trendBuckets(@Param("start") LocalDate start);

    /**
     * 近 30 天已支付渠道分布
     */
    @Select("SELECT COALESCE(NULLIF(TRIM(channel), ''), 'UNKNOWN') AS channelKey, COUNT(*) AS cnt, "
            + "COALESCE(SUM(pay_amount), 0) AS amt FROM cashier_orders "
            + "WHERE status IN ('PAID', 'SUCCESS') AND created_at >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) "
            + "GROUP BY COALESCE(NULLIF(TRIM(channel), ''), 'UNKNOWN')")
    List<Map<String, Object>> channelDistributionLast30Days();

    /**
     * 订单号 / 商户订单号模糊检索（管理端全局搜索）
     */
    @Select("SELECT order_id AS orderId, merchant_id AS merchantId, merchant_order_no AS merchantOrderNo, "
            + "status, amount, created_at AS createdAt FROM cashier_orders "
            + "WHERE order_id LIKE CONCAT('%', #{q}, '%') OR merchant_order_no LIKE CONCAT('%', #{q}, '%') "
            + "ORDER BY created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> quickSearch(@Param("q") String q, @Param("limit") int limit);
}
