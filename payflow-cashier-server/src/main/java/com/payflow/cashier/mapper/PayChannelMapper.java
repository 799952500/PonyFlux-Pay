package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.PayChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付渠道 Mapper
 *
 * @author PayFlow Team
 */
@Mapper
public interface PayChannelMapper extends BaseMapper<PayChannel> {
}
