package com.payflow.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理端从订单发起退款申请。
 */
@Data
public class AdminOrderRefundRequest {

    @NotBlank(message = "paymentId 不能为空")
    private String paymentId;

    @NotNull(message = "refundAmount 不能为空")
    @Min(value = 1, message = "退款金额必须大于0")
    private Long refundAmount;

    private String reason;
}
