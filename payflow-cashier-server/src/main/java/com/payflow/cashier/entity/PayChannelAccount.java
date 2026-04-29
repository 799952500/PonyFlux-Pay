package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.payflow.payment.core.ChannelConfigHolder;

import java.time.LocalDateTime;

/**
 * 渠道账户
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cashier_channel_accounts")
public class PayChannelAccount implements ChannelConfigHolder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属渠道ID */
    private Long channelId;

    /** 账户编码 */
    private String accountCode;

    /** 账户名称 */
    private String accountName;

    /** 渠道配置JSON（appId/appSecret/mchId等敏感信息） */
    private String channelConfig;

    /** 状态：ENABLED / DISABLED */
    private String status;

    /** 备注 */
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
