package com.payflow.recon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 手动触发对账请求。
 *
 * @author PayFlow Team
 */
@Data
@Schema(description = "手动触发对账")
public class RunReconRequest {

    @NotBlank
    @Schema(description = "对账渠道：alipay / wxpay", example = "alipay")
    private String reconChannel;

    @NotBlank
    @Schema(description = "渠道账户编码", example = "ALIPAY_ACC_001")
    private String accountCode;

    @NotNull
    @Schema(description = "账单日")
    private LocalDate billDate;
}
