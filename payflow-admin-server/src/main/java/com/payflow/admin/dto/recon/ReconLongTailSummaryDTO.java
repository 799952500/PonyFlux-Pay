package com.payflow.admin.dto.recon;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 长尾差异汇总。
 */
@Data
public class ReconLongTailSummaryDTO {

    private List<Bucket> buckets = new ArrayList<>();
    private int maxAgeDays;

    @Data
    public static class Bucket {
        private String ageBucket;
        private long diffCount;
        private long diffAmount;
    }
}
