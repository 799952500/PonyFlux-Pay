package com.payflow.admin.dto;

import lombok.Data;

@Data
public class DataIsolationCheckQueryDTO {

    private String classification;
    private String riskLevel;
    private String remediationStatus;
    private String targetType;
    private String merchantId;
    private Integer page = 1;
    private Integer size = 20;
}
