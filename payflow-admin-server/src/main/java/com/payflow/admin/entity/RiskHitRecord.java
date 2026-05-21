package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控命中记录。
 */
@Data
@TableName("admin_risk_hit_record")
public class RiskHitRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String traceId;

    private String merchantId;

    private String merchantName;

    private String orderId;

    private String merchantOrderNo;

    private Long ruleId;

    private String ruleCode;

    private String ruleName;

    private String ownerType;

    private String scopeType;

    private String action;

    private String decision;

    private String hitReason;

    private String requestSummary;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
