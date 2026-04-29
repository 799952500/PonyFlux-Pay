package com.payflow.admin.mapper.cashier;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.cashier.Payment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 支付记录 Mapper（cashier 库）
  * @author Lucas
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {

    /**
     * 按订单号查询支付记录
     */
    @Select("SELECT * FROM payments WHERE order_id = #{orderId} ORDER BY created_at DESC")
    List<Payment> findByOrderId(@Param("orderId") String orderId);

    /**
     * 按渠道查询支付记录
     */
    @Select("SELECT * FROM payments WHERE pay_channel = #{payChannel} ORDER BY created_at DESC")
    List<Payment> findByPayChannel(@Param("payChannel") String payChannel);
}
