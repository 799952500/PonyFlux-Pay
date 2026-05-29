package com.payflow.admin.dto.recon;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * SLA 规则保存请求。
 */
@Data
public class ReconDiffSlaRuleUpsertRequest {

    @NotNull(message = "enabled 不能为空")
    private Boolean enabled;

    @Positive(message = "slaHours 必须为正数")
    private Integer slaHours;

    private String escalateToRole;

    @DecimalMin(value = "0.01", message = "dueSoonRatio 最小为 0.01")
    @DecimalMax(value = "0.99", message = "dueSoonRatio 最大为 0.99")
    private BigDecimal dueSoonRatio;
}

