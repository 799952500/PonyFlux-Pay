package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控规则。
 *
 * @author Lucas
 */
@Data
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@TableName("risk_rules")
public class RiskRule {

    @TableId(type = IdType.AUTO)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    private String ruleCode;

    private String ruleName;

    private String ruleType;

    private String riskExpr;

    private BigDecimal threshold;

    private Long thresholdFen;

    private String unit;

    private String action;

    private Boolean enabled;

    private Integer priority;

    private String ownerType;

    private String ownerMerchantId;

    private String scopeType;

    private String description;

    private String createdBy;

    private String updatedBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
