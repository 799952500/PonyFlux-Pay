package com.payflow.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 风控规则保存请求。
 */
@Data
public class RiskRuleUpsertRequest {

    @NotBlank(message = "规则编码不能为空")
    @Size(max = 64, message = "规则编码不能超过 64 个字符")
    private String ruleCode;

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称不能超过 128 个字符")
    private String ruleName;

    @NotBlank(message = "规则类型不能为空")
    private String ruleType;

    @Size(max = 1024, message = "规则表达式不能超过 1024 个字符")
    private String riskExpr;

    @NotNull(message = "阈值不能为空")
    @Min(value = 1, message = "阈值必须大于 0")
    private Long thresholdFen;

    @NotBlank(message = "单位不能为空")
    private String unit;

    @NotBlank(message = "命中动作不能为空")
    private String action;

    @NotNull(message = "启用状态不能为空")
    private Boolean enabled;

    @NotNull(message = "优先级不能为空")
    @Min(value = 0, message = "优先级不能小于 0")
    private Integer priority;

    private String ownerType;

    private String scopeType;

    private List<String> scopeMerchantIds;

    @Size(max = 512, message = "描述不能超过 512 个字符")
    private String description;
}
