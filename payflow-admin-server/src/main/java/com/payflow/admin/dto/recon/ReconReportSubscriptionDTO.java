package com.payflow.admin.dto.recon;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 对账报告订阅 DTO。
 */
@Data
public class ReconReportSubscriptionDTO {

    private Long id;
    private String subscriberId;
    private String reportType;
    private String scope;
    private Boolean enabled;
    private LocalDateTime lastSentAt;
    private LocalDateTime createdAt;
}
