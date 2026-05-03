package com.payflow.admin.entity.cashier;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 退款记录（cashier 库 cashier_refunds）。
 *
 * @author Lucas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cashier_refunds")
public class Refund {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("refund_id")
    private String refundId;

    @TableField("payment_id")
    private String paymentId;

    @TableField("order_id")
    private String orderId;

    @TableField("pay_channel")
    private String payChannel;

    @TableField("refund_amount")
    private Long refundAmount;

    private String reason;

    private String status;

    @TableField("channel_refund_no")
    private String channelRefundNo;

    @TableField("merchant_refund_no")
    private String merchantRefundNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 退款处理中 */
    public static final String STATUS_REFUNDING = "REFUNDING";

    /** 退款成功 */
    public static final String STATUS_REFUNDED = "REFUNDED";

    /** 退款失败 */
    public static final String STATUS_FAILED = "FAILED";

    /** 退款关闭 */
    public static final String STATUS_CLOSED = "CLOSED";
}
