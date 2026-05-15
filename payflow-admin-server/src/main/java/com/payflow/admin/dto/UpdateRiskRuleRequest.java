package com.payflow.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新风控规则请求 DTO，替代 Map&lt;String, Object&gt; 参数。
 *
 * @author PayFlow Team
 */
@Data
public class UpdateRiskRuleRequest {

    @Size(max = 100, message = "规则名称长度不能超过100")
    private String ruleName;

    @Size(max = 50, message = "规则类型长度不能超过50")
    private String ruleType;

    private Boolean enabled;

    private BigDecimal threshold;

    @Size(max = 50, message = "单位长度不能超过50")
    private String unit;

    @Size(max = 200, message = "描述长度不能超过200")
    private String description;

    @Size(max = 50, message = "动作长度不能超过50")
    private String action;
}
