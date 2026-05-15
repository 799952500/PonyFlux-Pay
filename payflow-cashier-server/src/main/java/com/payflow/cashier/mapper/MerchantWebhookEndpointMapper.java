package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.MerchantWebhookEndpoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商户 Webhook 端点 Mapper。
 *
 * @author PayFlow Team
 */
@Mapper
public interface MerchantWebhookEndpointMapper extends BaseMapper<MerchantWebhookEndpoint> {
}
