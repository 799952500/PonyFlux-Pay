package com.payflow.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 风控规则状态切换请求。
 */
@Data
public class RiskRuleStatusRequest {

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;
}
