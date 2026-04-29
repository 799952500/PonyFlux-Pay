package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 退款记录实体。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cashier_refunds")
public class Refund {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 退款记录ID */
    private String refundId;

    /** 关联支付记录ID */
    private String paymentId;

    /** 关联订单号 */
    private String orderId;

    /** 支付渠道 */
    private String payChannel;

    /** 退款金额（分） */
    private Long refundAmount;

    /** 退款原因 */
    private String reason;

    /** 退款状态 */
    private String status;

    /** 渠道退款单号 */
    private String channelRefundNo;

    /** 商户侧退款单号 */
    private String merchantRefundNo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ==================== 状态常量 ====================

    /** 退款中 */
    public static final String STATUS_REFUNDING = "REFUNDING";

    /** 退款成功 */
    public static final String STATUS_REFUNDED = "REFUNDED";

    /** 退款失败 */
    public static final String STATUS_FAILED = "FAILED";

    /** 退款关闭 */
    public static final String STATUS_CLOSED = "CLOSED";
}
