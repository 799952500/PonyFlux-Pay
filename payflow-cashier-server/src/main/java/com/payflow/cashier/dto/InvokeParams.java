package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * App 调起参数
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InvokeParams", description = "App调起参数")
public class InvokeParams {

    @Schema(description = "调起方应用ID")
    private String appId;

    @Schema(description = "商户号")
    private String partnerId;

    @Schema(description = "预支付交易会话ID")
    private String prepayId;

    @Schema(description = "随机字符串")
    private String nonceStr;

    @Schema(description = "时间戳")
    private String timestamp;

    @Schema(description = "数据包")
    private String package_;

    @Schema(description = "签名")
    private String sign;
}
