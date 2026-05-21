package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控规则商户作用范围。
 */
@Data
@TableName("admin_risk_rule_merchant_scope")
public class RiskRuleMerchantScope {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ruleId;

    private String merchantId;

    private Boolean enabled;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
