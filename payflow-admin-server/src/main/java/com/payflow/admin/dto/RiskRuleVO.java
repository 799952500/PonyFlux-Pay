package com.payflow.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控规则展示对象。
 */
@Data
public class RiskRuleVO {

    private Long id;

    private String ruleCode;

    private String ruleName;

    private String ruleType;

    private String riskExpr;

    private Long thresholdFen;

    private String unit;

    private String action;

    private Boolean enabled;

    private Integer priority;

    private String ownerType;

    private String ownerMerchantId;

    private String ownerMerchantName;

    private String scopeType;

    private Integer scopeMerchantCount;

    private String description;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
