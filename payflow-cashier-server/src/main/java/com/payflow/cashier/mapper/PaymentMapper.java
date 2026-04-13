package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.Payment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper
 *
 * @author PayFlow Team
 */
@Mapper
public interface PaymentMapper extends BaseMapper<Payment> {
}
