package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建订单响应 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateOrderResponse", description = "创建订单响应")
public class CreateOrderResponse {

    /** 平台订单号 */
    @Schema(description = "平台订单号")
    private String orderId;

    /** 商户侧订单号 */
    @Schema(description = "商户侧订单号")
    private String merchantOrderNo;

    /** 商户号 */
    @Schema(description = "商户号")
    private String merchantId;

    /** 收银台跳转链接 */
    @Schema(description = "收银台跳转链接")
    private String payUrl;

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
}
