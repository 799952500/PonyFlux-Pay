package com.payflow.admin.entity.cashier;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付记录实体类（来自 cashier 库）
  * @author Lucas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("payments")
public class Payment {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("payment_id")
    private String paymentId;

    @TableField("order_id")
    private String orderId;

    @TableField("pay_channel")
    private String payChannel;

    @TableField("pay_method")
    private String payMethod;

    @TableField("channel_transaction_id")
    private String channelTransactionId;

    private Long amount;

    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    // 状态常量
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
}
