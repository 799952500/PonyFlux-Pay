package com.payflow.admin.dto;

import lombok.Data;

import java.util.List;

/**
 * 支付漏斗聚合结果 DTO。
 */
@Data
public class FunnelResult {

    private String dateFrom;

    private String dateTo;

    private List<FunnelStageDTO> stages;

    private Double overallConversionRate;

    private List<LossItem> lossBreakdown;

    @Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LossItem {
        private String name;
        private Long count;
        /** 占 CREATED 总数的百分比 */
        private Double percentage;
    }
}
