package com.payflow.admin.dto.recon;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 差异归因看板聚合结果。
 */
@Data
public class ReconDiffAggregationDTO {

    private List<MatrixCell> matrix = new ArrayList<>();
    private List<TrendPoint> trend = new ArrayList<>();
    private List<TopItem> topMerchants = new ArrayList<>();
    private List<TopItem> topAccounts = new ArrayList<>();
    private SlaStats slaStats = new SlaStats();

    @Data
    public static class MatrixCell {
        private String channel;
        private String diffType;
        private long diffCount;
        private long diffAmount;
    }

    @Data
    public static class TrendPoint {
        private String period;
        private long diffCount;
        private long diffAmount;
    }

    @Data
    public static class TopItem {
        private String key;
        private long diffCount;
        private long diffAmount;
    }

    @Data
    public static class SlaStats {
        private Double avgHandleMinutes;
        private Double slaMetRate;
        private Double longTailRate;
        private long sample;
    }
}
