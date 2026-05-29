package com.payflow.admin.dto.recon;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SLA 规则 DTO。
 */
@Data
public class ReconDiffSlaRuleDTO {
    private String diffType;
    private Boolean enabled;
    private Integer slaHours;
    private BigDecimal dueSoonRatio;
    private String escalateToRole;
    private String updatedBy;
    private LocalDateTime updatedAt;
}

