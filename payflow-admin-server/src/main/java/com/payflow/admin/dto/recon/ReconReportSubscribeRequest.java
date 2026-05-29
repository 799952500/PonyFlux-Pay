package com.payflow.admin.dto.recon;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 报告订阅创建请求。
 */
@Data
public class ReconReportSubscribeRequest {

    @NotBlank(message = "reportType 不能为空")
    private String reportType;

    @NotBlank(message = "scope 不能为空")
    private String scope;

    private Boolean enabled = true;
}
