package com.payflow.admin.entity.cashier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 收银台渠道（payflow_cashier.cashier_channels）
 */
@Data
@TableName("cashier_channels")
public class CashierChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String channelCode;

    private String channelName;

    private String status;
}
