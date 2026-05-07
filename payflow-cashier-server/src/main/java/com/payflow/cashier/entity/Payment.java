package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 支付记录实体类
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("cashier_payments")
@Schema(name = "Payment", description = "支付记录实体")
public class Payment {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("payment_id")
    @Schema(description = "支付记录ID")
    private String paymentId;

    @TableField("order_id")
    @Schema(description = "关联订单号")
    private String orderId;

    @TableField("pay_channel")
    @Schema(description = "支付渠道")
    private String payChannel;

    /** 收款渠道账户编码（下单路由结果，用于对账按账号汇总） */
    @TableField("account_code")
    @Schema(description = "收款账户编码")
    private String accountCode;

    @TableField("pay_method")
    @Schema(description = "支付方式")
    private String payMethod;

    @TableField("channel_transaction_id")
    @Schema(description = "第三方交易流水号")
    private String channelTransactionId;

    @Schema(description = "支付金额（分）")
    private Long amount;

    @Schema(description = "支付状态")
    private String status;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // ==================== 状态常量 ====================

    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_REFUNDED = "REFUNDED";
    /** 部分退款成功，仍可继续退款 */
    public static final String STATUS_PARTIAL_REFUND = "PARTIAL_REFUND";

    // ==================== 支付方式常量 ====================

    public static final String METHOD_WECHAT_APP = "WECHAT_APP";
    public static final String METHOD_WECHAT_H5 = "WECHAT_H5";
    public static final String METHOD_WECHAT_NATIVE = "WECHAT_NATIVE";
    public static final String METHOD_WECHAT_JSAPI = "WECHAT_JSAPI";
    public static final String METHOD_WECHAT_MINI = "WECHAT_MINI";
    public static final String METHOD_WECHAT_MICROPAY = "WECHAT_MICROPAY";
    public static final String METHOD_ALIPAY_APP = "ALIPAY_APP";
    public static final String METHOD_ALIPAY_WAP = "ALIPAY_WAP";
    public static final String METHOD_BANK_CARD = "BANK_CARD";
    public static final String METHOD_ALIPAY_FACE = "ALIPAY_FACE";
    public static final String METHOD_UNION_H5 = "UNION_H5";
}
