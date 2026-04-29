package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.Refund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款记录 Mapper。
 *
 * @author PayFlow Team
 */
@Mapper
public interface RefundMapper extends BaseMapper<Refund> {
}
