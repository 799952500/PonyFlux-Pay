package com.payflow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 漏斗单个阶段 DTO。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FunnelStageDTO {

    private String name;

    private Long count;

    /** 相邻阶段转化率（%），第一阶段为 null */
    private Double rate;
}
