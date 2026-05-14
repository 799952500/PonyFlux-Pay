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
     * 聚合查询：按时间范围统计交易数据（用于写入 admin_dashboard_metrics）
     */
    @Select("SELECT DATE_FORMAT(p.created_at, #{timeFormat}) AS timeBucket, "
            + "COALESCE(NULLIF(TRIM(p.pay_channel), ''), 'ALL') AS channelCode, "
            + "COALESCE(SUM(p.amount), 0) AS totalAmount, "
            + "COUNT(DISTINCT p.payment_id) AS totalCount, "
            + "COUNT(DISTINCT o.merchant_id) AS activeMerchants "
            + "FROM cashier_payments p "
            + "JOIN cashier_orders o ON p.order_id = o.order_id "
            + "WHERE p.status = 'SUCCESS' AND p.created_at BETWEEN #{startTime} AND #{endTime} "
            + "GROUP BY timeBucket, channelCode")
    List<Map<String, Object>> aggregatePayments(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime,
                                                 @Param("timeFormat") String timeFormat);

    /**
     * 聚合查询：按时间范围统计退款数据
     */
    @Select("SELECT DATE_FORMAT(r.created_at, #{timeFormat}) AS timeBucket, "
            + "COALESCE(NULLIF(TRIM(r.refund_channel), ''), 'ALL') AS channelCode, "
            + "COALESCE(SUM(r.refund_amount), 0) AS refundAmount, "
            + "COUNT(DISTINCT r.refund_id) AS refundCount "
            + "FROM cashier_refunds r "
            + "WHERE r.status = 'SUCCESS' AND r.created_at BETWEEN #{startTime} AND #{endTime} "
            + "GROUP BY timeBucket, channelCode")
    List<Map<String, Object>> aggregateRefunds(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("timeFormat") String timeFormat);

    /**
     * 商户交易排行：指定时间范围内按商户交易额降序
     */
    @Select("SELECT o.merchant_id AS merchantId, "
            + "COALESCE(SUM(p.amount), 0) AS totalAmount, "
            + "COUNT(DISTINCT p.payment_id) AS totalCount "
            + "FROM cashier_payments p "
            + "JOIN cashier_orders o ON p.order_id = o.order_id "
            + "WHERE p.status = 'SUCCESS' AND p.created_at BETWEEN #{startTime} AND #{endTime} "
            + "GROUP BY o.merchant_id "
            + "ORDER BY totalAmount DESC LIMIT #{limit}")
    List<Map<String, Object>> merchantRanking(@Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime,
                                               @Param("limit") int limit);

    /**
     * 商户近30天每日交易趋势
     */
    @Select("SELECT DATE(o.created_at) AS date, COUNT(*) AS orders, "
            + "COALESCE(SUM(o.pay_amount), 0) AS revenue "
            + "FROM cashier_orders o "
            + "WHERE o.merchant_id = #{merchantId} AND o.status IN ('PAID', 'SUCCESS') "
            + "AND o.created_at >= #{startDate} "
            + "GROUP BY DATE(o.created_at) ORDER BY date")
    List<Map<String, Object>> merchantTrend30Days(@Param("merchantId") String merchantId,
                                                    @Param("startDate") LocalDate startDate);

    /**
     * 商户渠道偏好分布
     */
    @Select("SELECT COALESCE(NULLIF(TRIM(o.channel), ''), 'UNKNOWN') AS channel, "
            + "COUNT(*) AS cnt, COALESCE(SUM(o.pay_amount), 0) AS amount "
            + "FROM cashier_orders o "
            + "WHERE o.merchant_id = #{merchantId} AND o.status IN ('PAID', 'SUCCESS') "
            + "AND o.created_at >= #{startDate} "
            + "GROUP BY COALESCE(NULLIF(TRIM(o.channel), ''), 'UNKNOWN') "
            + "ORDER BY cnt DESC")
    List<Map<String, Object>> merchantChannelPrefs(@Param("merchantId") String merchantId,
                                                     @Param("startDate") LocalDate startDate);

    /**
     * 商户指定日期范围内的订单日均笔数（用于流失预警）
     */
    @Select("SELECT o.merchant_id AS merchantId, "
            + "CAST(COUNT(*) AS DECIMAL(10,2)) / GREATEST(DATEDIFF(#{endDate}, #{startDate}), 1) AS dailyAvg, "
            + "COUNT(DISTINCT DATE(o.created_at)) AS consecutiveDays "
            + "FROM cashier_orders o "
            + "WHERE o.created_at BETWEEN #{startDate} AND #{endDate} "
            + "AND o.merchant_id IS NOT NULL "
            + "GROUP BY o.merchant_id "
            + "HAVING COUNT(*) >= 3")
    List<Map<String, Object>> merchantOrderCountsInRange(@Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate);

    /**
     * 商户退款率
     */
    @Select("SELECT COUNT(DISTINCT r.refund_id) AS refundCount, "
            + "COUNT(DISTINCT p.payment_id) AS totalCount, "
            + "CONCAT(ROUND(COUNT(DISTINCT r.refund_id) * 100.0 / NULLIF(COUNT(DISTINCT p.payment_id), 0), 1), '%') AS rate "
            + "FROM cashier_payments p "
            + "LEFT JOIN cashier_refunds r ON r.order_id = p.order_id AND r.status = 'SUCCESS' "
            + "JOIN cashier_orders o ON p.order_id = o.order_id "
            + "WHERE o.merchant_id = #{merchantId} AND p.status = 'SUCCESS' "
            + "AND p.created_at >= #{startDate}")
    Map<String, Object> merchantRefundRate(@Param("merchantId") String merchantId,
                                            @Param("startDate") LocalDate startDate);

    /**
     * 订单号 / 商户订单号模糊检索（管理端全局搜索）
     */
    @Select("SELECT order_id AS orderId, merchant_id AS merchantId, merchant_order_no AS merchantOrderNo, "
            + "status, amount, created_at AS createdAt FROM cashier_orders "
            + "WHERE order_id LIKE CONCAT('%', #{q}, '%') OR merchant_order_no LIKE CONCAT('%', #{q}, '%') "
            + "ORDER BY created_at DESC LIMIT #{limit}")
    List<Map<String, Object>> quickSearch(@Param("q") String q, @Param("limit") int limit);
}
