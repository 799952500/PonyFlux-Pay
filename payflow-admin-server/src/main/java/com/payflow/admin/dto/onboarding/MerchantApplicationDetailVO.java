package com.payflow.admin.dto.onboarding;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理端进件申请详情。
 */
@Data
@Builder
public class MerchantApplicationDetailVO {

    private Long id;

    private String applicationNo;

    private String merchantName;

    private String status;

    private String applicationSource;

    private String bizLicenseNo;

    private String contactName;

    private String contactPhone;

    private String contactEmail;

    private String allocatedMerchantId;

    private String payloadJson;

    private String rejectReason;

    private Integer resultQueryCount;

    private LocalDateTime secretViewedAt;

    private Long approverId;

    private LocalDateTime approvedAt;

    private LocalDateTime rejectedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
