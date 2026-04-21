package com.payflow.admin.entity.cashier;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 订单实体类（来自 cashier 库）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("orders")
public class Order {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("order_id")
    private String orderId;

    private String merchantId;

    @TableField("merchant_order_no")
    private String merchantOrderNo;

    private Long amount;

    private String currency;

    @TableField("pay_amount")
    private Long payAmount;

    private String subject;

    private String body;

    private String attach;

    private String channel;

    private String status;

    @TableField("notify_url")
    private String notifyUrl;

    @TableField("merchant_notify_url")
    private String merchantNotifyUrl;

    @TableField("return_url")
    private String returnUrl;

    @TableField("expire_time")
    private LocalDateTime expireTime;

    @TableField("pay_time")
    private LocalDateTime payTime;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("notify_status")
    private String notifyStatus;

    @TableField("notify_retry_count")
    private Integer notifyRetryCount;

    // 状态常量
    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_PAYING = "PAYING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REFUNDED = "REFUNDED";
}
