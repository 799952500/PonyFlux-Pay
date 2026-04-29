package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 收银台响应 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CashierResponse", description = "收银台响应")
public class CashierResponse {

    /** 平台订单号 */
    @Schema(description = "平台订单号")
    private String orderId;

    /** 商户名称 */
    @Schema(description = "商户名称")
    private String merchantName;

    /** 订单标题 */
    @Schema(description = "订单标题")
    private String subject;

    /** 订单金额（分） */
    @Schema(description = "订单金额（分）")
    private Long amount;

    /** 币种 */
    @Schema(description = "币种")
    private String currency;

    /** 过期时间 */
    @Schema(description = "过期时间")
    private String expireTime;

    /** 订单状态 */
    @Schema(description = "订单状态")
    private String status;

    /** 可用支付方式列表 */
    @Schema(description = "可用支付方式列表")
    private List<PaymentMethodDTO> paymentMethods;

    /** 商户支付成功跳转地址 */
    @Schema(description = "商户支付成功跳转地址")
    private String successUrl;

    /** 商户支付失败跳转地址 */
    @Schema(description = "商户支付失败跳转地址")
    private String failUrl;
}
