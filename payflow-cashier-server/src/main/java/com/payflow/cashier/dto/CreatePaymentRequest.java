package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发起支付请求 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreatePaymentRequest", description = "发起支付请求")
public class CreatePaymentRequest {

    @NotBlank(message = "orderId 不能为空")
    @Schema(description = "平台订单号")
    private String orderId;

    @NotBlank(message = "payChannel 不能为空")
    @Schema(description = "支付渠道")
    private String payChannel;

    @NotBlank(message = "payMethod 不能为空")
    @Schema(description = "支付方式")
    private String payMethod;

    @NotBlank(message = "deviceType 不能为空")
    @Schema(description = "设备类型")
    private String deviceType;

    @Schema(description = "客户端 IP")
    private String clientIp;

    @Schema(description = "微信 OpenId")
    private String openId;

    /**
     * 微信/支付宝付款码（条码支付、被扫场景）。
     */
    @Schema(description = "付款码（微信付款码或支付宝条码）")
    private String authCode;

    @Schema(description = "支付宝 UserId")
    private String alipayUserId;

    @Schema(description = "支付完成跳转地址（WAP支付）")
    private String returnUrl;
}
