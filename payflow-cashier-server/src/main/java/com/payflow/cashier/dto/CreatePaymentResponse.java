package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发起支付响应 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreatePaymentResponse", description = "发起支付响应")
public class CreatePaymentResponse {

    @Schema(description = "支付记录ID")
    private String paymentId;

    @Schema(description = "关联订单号")
    private String orderId;

    @Builder.Default
    @Schema(description = "支付状态")
    private String status = "PROCESSING";

    @Schema(description = "调起动作类型")
    private String action;

    @Schema(description = "App调起参数")
    private InvokeParams invokeParams;

    @Schema(description = "扫码支付URL")
    private String qrCodeUrl;

    @Schema(description = "扫码二维码Base64图片")
    private String qrCodeImage;

    @Schema(description = "REDIRECT 调起URL")
    private String redirectUrl;

    @Schema(description = "FORM HTML 表单")
    private String formHtml;

    public static final String ACTION_INVOKE = "INVOKE";
    public static final String ACTION_QR_CODE = "QR_CODE";
    public static final String ACTION_REDIRECT = "REDIRECT";
    public static final String ACTION_FORM = "FORM";
}
