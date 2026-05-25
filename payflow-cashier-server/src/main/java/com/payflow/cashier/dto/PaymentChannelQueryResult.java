package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 向支付机构查单结果（管理端运维）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PaymentChannelQueryResult", description = "渠道查单结果")
public class PaymentChannelQueryResult {

    @Schema(description = "支付记录 ID")
    private String paymentId;

    @Schema(description = "订单号")
    private String orderId;

    @Schema(description = "支付渠道编码")
    private String payChannel;

    @Schema(description = "本地支付状态")
    private String localStatus;

    @Schema(description = "渠道侧是否已支付成功")
    private Boolean channelPaid;

    @Schema(description = "是否已执行本地同步")
    private Boolean synced;

    @Schema(description = "当前渠道是否支持主动查单")
    private Boolean channelQuerySupported;

    @Schema(description = "说明信息")
    private String message;
}
