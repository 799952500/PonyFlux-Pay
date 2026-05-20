package com.payflow.admin.entity.cashier;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 收银台渠道账户（payflow_cashier.cashier_channel_accounts）
 */
@Data
@TableName("cashier_channel_accounts")
public class CashierChannelAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;

    private String accountCode;

    private String accountName;

    private String status;
}
