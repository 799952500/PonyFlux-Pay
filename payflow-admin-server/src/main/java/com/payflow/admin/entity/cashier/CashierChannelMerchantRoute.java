package com.payflow.admin.entity.cashier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收银台商户渠道路由（payflow_cashier.cashier_channel_merchant_routes）
 */
@Data
@TableName("cashier_channel_merchant_routes")
public class CashierChannelMerchantRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelAccountId;

    private String merchantId;

    private Boolean enabled;

    private Integer priority;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
