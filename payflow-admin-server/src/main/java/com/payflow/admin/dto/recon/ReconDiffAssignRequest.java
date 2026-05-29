package com.payflow.admin.dto.recon;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 工单指派请求。
 */
@Data
public class ReconDiffAssignRequest {

    @NotBlank(message = "assigneeId 不能为空")
    private String assigneeId;

    private String remark;
}

