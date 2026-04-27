package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商户渠道路由
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cashier_channel_merchant_routes")
public class PayChannelMerchantRoute {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 渠道账户ID */
    private Long channelAccountId;

    /** 商户号 */
    private String merchantId;

    /** 是否启用 */
    private Boolean enabled;

    /** 优先级，默认0，数字越大优先级越高 */
    private Integer priority;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
