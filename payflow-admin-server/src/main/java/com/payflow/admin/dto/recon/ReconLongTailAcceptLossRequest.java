package com.payflow.admin.dto.recon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 批量挂账请求。
 */
@Data
public class ReconLongTailAcceptLossRequest {

    @NotEmpty(message = "diffIds 不能为空")
    @Size(min = 1, max = 50, message = "diffIds 数量须在 1-50 之间")
    private List<Long> diffIds;

    @NotBlank(message = "remark 不能为空")
    private String remark;
}
