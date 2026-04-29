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
 * 创建订单请求 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateOrderRequest", description = "创建订单请求参数")
public class CreateOrderRequest {

    /** 商户号（从 JWT 自动注入，可覆盖） */
    @Schema(description = "商户号")
    private String merchantId;

    /** 商户侧订单号 */
    @NotBlank(message = "merchantOrderNo 不能为空")
    @Schema(description = "商户侧订单号")
    private String merchantOrderNo;

    /** 订单金额（分） */
    @NotNull(message = "amount 不能为空")
    @Min(value = 1, message = "金额最小为 1 分")
    @Schema(description = "订单金额（分）")
    private Long amount;

    /** 币种 */
    @Builder.Default
    @Schema(description = "币种")
    private String currency = "CNY";

    /** 支付渠道 */
    @NotBlank(message = "channel 不能为空")
    @Schema(description = "支付渠道")
    private String channel;

    /** 商户异步通知地址 */
    @NotBlank(message = "notifyUrl 不能为空")
    @Schema(description = "商户异步通知地址")
    private String notifyUrl;

    /** 支付完成回跳地址 */
    @Schema(description = "支付完成回跳地址")
    private String returnUrl;

    /** 商户支付成功跳转地址（用于订单支付成功后的前端跳转） */
    @Schema(description = "商户支付成功跳转地址")
    private String successUrl;

    /** 商户支付失败跳转地址（用于订单支付失败后的前端跳转） */
    @Schema(description = "商户支付失败跳转地址")
    private String failUrl;

    /** 订单过期时间（分钟） */
    @Builder.Default
    @Schema(description = "订单过期时间（分钟）")
    private Integer expireMinutes = 30;

    /** 订单标题 */
    @NotBlank(message = "subject 不能为空")
    @Schema(description = "订单标题")
    private String subject;

    /** 订单详情 */
    @Schema(description = "订单详情")
    private String body;

    /** 透传字段 */
    @Schema(description = "透传字段")
    private String attach;

    /** 客户端 IP */
    @Schema(description = "客户端 IP")
    private String clientIp;
}
