package com.payflow.admin.mapper.cashier;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.cashier.Refund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款 Mapper（cashier 库）。
 *
 * @author Lucas
 */
@Mapper
public interface RefundMapper extends BaseMapper<Refund> {
}
