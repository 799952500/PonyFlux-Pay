package com.payflow.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理端处理对账差异请求。
 *
 * @author PayFlow Team
 */
@Data
public class HandleReconDiffRequest {

    @NotBlank
    private String action;

    private String remark;
}
