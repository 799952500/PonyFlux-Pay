package com.payflow.admin.dto.recon;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工单终态提交请求。
 */
@Data
public class ReconDiffCompleteRequest {

    @NotBlank(message = "action 不能为空")
    private String action;

    @NotBlank(message = "remark 不能为空")
    private String remark;
}

