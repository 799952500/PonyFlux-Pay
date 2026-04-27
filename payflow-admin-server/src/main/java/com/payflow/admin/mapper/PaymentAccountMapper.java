package com.payflow.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.admin.entity.PaymentAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface PaymentAccountMapper extends BaseMapper<PaymentAccount> {

    @Select("SELECT pa.*, c.channel_name " +
            "FROM payment_accounts pa " +
            "LEFT JOIN channels c ON pa.channel_id = c.id " +
            "ORDER BY pa.id")
    List<PaymentAccount> listWithChannelName();

    @Select("SELECT r.id, r.merchant_id, r.channel_id, r.payment_account_id, " +
            "       r.enabled, r.priority, r.description, r.created_at, r.updated_at, " +
            "       m.merchant_name, c.channel_name, " +
            "       pa.account_code, pa.account_name " +
            "FROM admin_channel_routes r " +
            "LEFT JOIN merchants m ON r.merchant_id = m.merchant_id " +
            "LEFT JOIN channels c ON r.channel_id = c.id " +
            "LEFT JOIN payment_accounts pa ON r.payment_account_id = pa.id " +
            "ORDER BY r.priority DESC, r.id ASC")
    List<Map<String, Object>> channelRouteListWithDetails();
}

