package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款请求 DTO。
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RefundRequest", description = "退款请求")
public class RefundRequest {

    @NotBlank(message = "paymentId 不能为空")
    @Schema(description = "支付记录ID")
    private String paymentId;

    @NotNull(message = "refundAmount 不能为空")
    @Min(value = 1, message = "退款金额必须大于0")
    @Schema(description = "退款金额（分）")
    private Long refundAmount;

    @Schema(description = "退款原因")
    private String reason;

    @Schema(description = "商户侧退款单号（可选）")
    private String merchantRefundNo;
}
