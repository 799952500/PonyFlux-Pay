package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 订单 Mapper
 *
 * @author PayFlow Team
 */
@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT COALESCE(SUM(amount), 0) FROM cashier_orders " +
            "WHERE merchant_id = #{merchantId} " +
            "AND created_at >= #{startTime} AND created_at < #{endTime} " +
            "AND status IN ('CREATED','PAYING','PAID')")
    Long sumAmountByMerchantAndTimeRange(@Param("merchantId") String merchantId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}
