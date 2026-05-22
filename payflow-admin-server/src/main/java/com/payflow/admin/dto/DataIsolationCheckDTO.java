package com.payflow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataIsolationCheckDTO {

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
