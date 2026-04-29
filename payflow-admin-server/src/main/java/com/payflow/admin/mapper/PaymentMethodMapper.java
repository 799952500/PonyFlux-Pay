package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.PaymentMethod;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
/**
 * @author Lucas
 */
public interface PaymentMethodMapper extends BaseMapper<PaymentMethod> {
    
    @Select("SELECT pm.*, c.channel_name, c.channel_type " +
            "FROM payment_methods pm " +
            "LEFT JOIN channels c ON pm.channel_id = c.id " +
            "ORDER BY pm.id")
    List<PaymentMethod> listWithChannelName();
}