package com.payflow.cashier.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 风控规则。
 *
 * @author Lucas
 */
@Data
@TableName("cashier_risk_rules")
@Schema(name = "RiskRule", description = "风控规则")
public class RiskRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String ruleCode;

    private String ruleName;

    private String ruleType;

    /**
     * QLExpress 脚本；ruleType=CUSTOM 且非空时执行，返回 true 表示放行，false 表示触发 action。
     */
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

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}

