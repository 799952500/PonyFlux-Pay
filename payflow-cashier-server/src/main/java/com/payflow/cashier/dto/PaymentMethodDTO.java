package com.payflow.cashier.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付方式 DTO
 *
 * @author PayFlow Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "PaymentMethodDTO", description = "支付方式")
public class PaymentMethodDTO {

    @Schema(description = "支付方式编码")
    private String code;

    @Schema(description = "支付方式名称")
    private String name;

    @Schema(description = "图标路径")
    private String icon;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "是否推荐")
    private Boolean recommended;
}
