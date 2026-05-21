package com.payflow.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理端风控规则查询请求。
 */
@Data
public class RiskRuleQueryRequest {

    @Min(1)
    private Integer page = 1;

    @Min(1)
    @Max(100)
    private Integer pageSize = 20;

    private String merchantId;

    private String ownerType;

    private String scopeType;

    private String ruleType;

    private Boolean enabled;

    private String keyword;
}
