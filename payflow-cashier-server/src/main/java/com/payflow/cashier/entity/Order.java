package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单实体类
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cashier_orders")
@Schema(name = "Order", description = "订单实体")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("order_id")
    @Schema(description = "平台订单号")
    private String orderId;

    @Schema(description = "商户号")
    private String merchantId;

    @TableField("merchant_order_no")
    @Schema(description = "商户侧订单号")
    private String merchantOrderNo;

    @Schema(description = "订单金额（分）")
    private Long amount;

    @Schema(description = "币种")
    private String currency;

    @TableField("pay_amount")
    @Schema(description = "实付金额（分）")
    private Long payAmount;

    @Schema(description = "订单标题")
    private String subject;

    @Schema(description = "订单详情")
    private String body;

    @Schema(description = "透传字段")
    private String attach;

    @Schema(description = "支付渠道")
    private String channel;

    @Schema(description = "订单状态")
    private String status;

    @TableField("notify_url")
    @Schema(description = "商户异步通知地址")
    private String notifyUrl;

    @TableField("merchant_notify_url")
    @Schema(description = "商户异步回调地址（支付成功后回调）")
    private String merchantNotifyUrl;

    @TableField("return_url")
    @Schema(description = "支付完成回跳地址")
    private String returnUrl;

    @TableField("success_url")
    @Schema(description = "商户支付成功跳转地址")
    private String successUrl;

    @TableField("fail_url")
    @Schema(description = "商户支付失败跳转地址")
    private String failUrl;

    @TableField("expire_time")
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;

    @TableField("pay_time")
    @Schema(description = "支付时间")
    private LocalDateTime payTime;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @TableField("notify_status")
    @Schema(description = "回调状态：PENDING/SUCCESS/FAILED")
    private String notifyStatus;

    @TableField("notify_retry_count")
    @Schema(description = "回调重试次数")
    private Integer notifyRetryCount;

    // ==================== 状态常量 ====================

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_PAYING = "PAYING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REFUNDED = "REFUNDED";

    // ==================== 回调状态常量 ====================
    public static final String NOTIFY_STATUS_PENDING = "PENDING";
    public static final String NOTIFY_STATUS_SUCCESS = "SUCCESS";
    public static final String NOTIFY_STATUS_FAILED = "FAILED";

    // ==================== 渠道常量 ====================

    public static final String CHANNEL_WECHAT_PAY = "WECHAT_PAY";
    public static final String CHANNEL_ALIPAY = "ALIPAY";
    public static final String CHANNEL_UNION_PAY = "UNION_PAY";
}
