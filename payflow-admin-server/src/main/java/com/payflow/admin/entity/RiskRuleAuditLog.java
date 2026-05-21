package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控规则变更审计。
 */
@Data
@TableName("admin_risk_rule_audit_log")
public class RiskRuleAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private String operatorId;

    private String operatorName;

    private String operatorType;

    private String merchantId;

    private String operationType;

    private String beforeSummary;

    private String afterSummary;

    private String clientIp;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
