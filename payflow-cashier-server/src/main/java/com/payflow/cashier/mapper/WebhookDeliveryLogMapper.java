package com.payflow.cashier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.payflow.cashier.entity.WebhookDeliveryLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * Webhook 投递日志 Mapper。
 *
 * @author PayFlow Team
 */
@Mapper
public interface WebhookDeliveryLogMapper extends BaseMapper<WebhookDeliveryLog> {
}
