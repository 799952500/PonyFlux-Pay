package com.payflow.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_data_isolation_checks")
public class DataIsolationCheck {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String checkId;
    private String targetType;
    private String targetName;
    private String classification;
    private String merchantFieldStatus;
    private String riskLevel;
    private String affectedEntries;
    private String remediationStatus;
    private String decisionReason;
    private String merchantId;
    private LocalDateTime lastScannedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
