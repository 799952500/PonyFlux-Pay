package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建 Payment Link 请求。
 */
@Data
@Schema(name = "PaymentLinkCreateRequest")
public class PaymentLinkCreateRequest {

    @NotBlank
    @Schema(description = "标题")
    private String title;

    @Schema(description = "固定金额（分），为空表示用户输入")
    private Long amount;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "最大使用次数")
    private Integer maxUse;

    @Schema(description = "过期时间")
    private LocalDateTime expireAt;
}
