package com.payflow.recon.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 差异处理请求。
 *
 * @author PayFlow Team
 */
@Data
@Schema(description = "差异处理")
public class HandleDiffRequest {

    @NotBlank
    @Schema(description = "PROCESSED / IGNORED", example = "PROCESSED")
    private String action;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "操作人（可由调用方传入）")
    private String operator;
}
