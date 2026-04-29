package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款响应 DTO。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RefundResponse", description = "退款响应")
public class RefundResponse {

    /** 退款记录ID */
    private String refundId;

    /** 关联支付记录ID */
    private String paymentId;

    /** 渠道退款单号 */
    private String channelRefundNo;

    /** 退款状态 */
    private String status;

    /** 退款金额（分） */
    private Long refundAmount;

    /** 错误信息（失败时填充） */
    private String errorMsg;
}
