package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 订单详情响应 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OrderDetailResponse", description = "订单详情响应")
public class OrderDetailResponse {

    @Schema(description = "平台订单号")
    private String orderId;

    @Schema(description = "商户号")
    private String merchantId;

    @Schema(description = "商户侧订单号")
    private String merchantOrderNo;

    @Schema(description = "订单金额（分）")
    private Long amount;

    @Schema(description = "实付金额（分）")
    private Long payAmount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "订单标题")
    private String subject;

    @Schema(description = "订单详情")
    private String body;

    @Schema(description = "透传字段")
    private String attach;

    @Schema(description = "支付渠道")
    private String channel;

    @Schema(description = "订单状态")
    private String status;

    @Schema(description = "过期时间")
    private String expireTime;

    @Schema(description = "支付时间")
    private String payTime;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "关联支付记录")
    private List<PaymentRecordDTO> payments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentRecordDTO {
        private String paymentId;
        private String payChannel;
        private String payMethod;
        private Long amount;
        private String status;
        private String channelTransactionId;
        private String createdAt;
        private String updatedAt;
    }
}
