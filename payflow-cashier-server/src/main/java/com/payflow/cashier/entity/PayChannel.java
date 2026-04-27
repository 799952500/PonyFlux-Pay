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
 * 支付渠道
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cashier_channels")
public class PayChannel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 渠道编码：wechat_pay / alipay / union_pay */
    private String channelCode;

    /** 渠道名称：微信支付 / 支付宝 / 银联 */
    private String channelName;

    /** 图标URL */
    private String iconUrl;

    /** 状态：ENABLED / DISABLED */
    private String status;

    /** 排序权重，越大越靠前 */
    private Integer sortWeight;

    /** 描述 */
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
