package com.payflow.admin.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 风控命中记录展示对象。
 */
@Data
public class RiskHitRecordVO {

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

    private LocalDateTime createdAt;
}
