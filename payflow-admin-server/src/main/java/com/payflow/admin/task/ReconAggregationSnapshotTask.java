package com.payflow.admin.task;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.admin.entity.recon.ReconAggregationSnapshotEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.mapper.recon.ReconAggregationSnapshotEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 每日预聚合快照（02:00，T-1）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReconAggregationSnapshotTask {

    private final ReconTaskEntityMapper reconTaskEntityMapper;
    private final ReconDiffEntityMapper reconDiffEntityMapper;
    private final ReconAggregationSnapshotEntityMapper snapshotMapper;

    @Scheduled(cron = "0 0 2 * * ?")
    public void snapshotYesterday() {
        LocalDate statDate = LocalDate.now().minusDays(1);
        try {
            List<ReconTaskEntity> tasks = reconTaskEntityMapper.selectList(
                    Wrappers.<ReconTaskEntity>lambdaQuery().eq(ReconTaskEntity::getBillDate, statDate));
            if (tasks.isEmpty()) {
                return;
            }
            Map<String, ReconTaskEntity> taskById = tasks.stream()
                    .collect(Collectors.toMap(ReconTaskEntity::getTaskId, t -> t, (a, b) -> a));
            List<ReconDiffEntity> diffs = reconDiffEntityMapper.selectList(
                    Wrappers.<ReconDiffEntity>lambdaQuery().in(ReconDiffEntity::getTaskId, taskById.keySet()));

            Map<String, ReconAggregationSnapshotEntity> agg = new HashMap<>();
            LocalDateTime now = LocalDateTime.now();
            for (ReconDiffEntity d : diffs) {
                ReconTaskEntity t = taskById.get(d.getTaskId());
                if (t == null) {
                    continue;
                }
                String merchantId = d.getMerchantId() != null ? d.getMerchantId() : "UNKNOWN";
                String key = statDate + "|" + merchantId + "|" + t.getChannel() + "|" + d.getDiffType();
                ReconAggregationSnapshotEntity row = agg.computeIfAbsent(key, k -> {
                    ReconAggregationSnapshotEntity e = new ReconAggregationSnapshotEntity();
                    e.setStatDate(statDate);
                    e.setMerchantId(merchantId);
                    e.setChannel(t.getChannel());
                    e.setDiffType(d.getDiffType());
                    e.setDiffCount(0L);
                    e.setDiffAmount(0L);
                    e.setProcessedCount(0L);
                    e.setIgnoredCount(0L);
                    e.setAcceptedLossCount(0L);
                    e.setSlaMetCount(0L);
                    e.setSlaTotalCount(0L);
                    e.setCreatedAt(now);
                    e.setUpdatedAt(now);
                    return e;
                });
                row.setDiffCount(row.getDiffCount() + 1);
                long amt = Math.max(
                        d.getChannelAmount() != null ? d.getChannelAmount() : 0L,
                        d.getLocalAmount() != null ? d.getLocalAmount() : 0L);
                row.setDiffAmount(row.getDiffAmount() + amt);
            }
            for (ReconAggregationSnapshotEntity row : agg.values()) {
                ReconAggregationSnapshotEntity existing = snapshotMapper.selectOne(
                        Wrappers.<ReconAggregationSnapshotEntity>lambdaQuery()
                                .eq(ReconAggregationSnapshotEntity::getStatDate, row.getStatDate())
                                .eq(ReconAggregationSnapshotEntity::getMerchantId, row.getMerchantId())
                                .eq(ReconAggregationSnapshotEntity::getChannel, row.getChannel())
                                .eq(ReconAggregationSnapshotEntity::getDiffType, row.getDiffType()));
                if (existing == null) {
                    snapshotMapper.insert(row);
                } else {
                    row.setId(existing.getId());
                    snapshotMapper.updateById(row);
                }
            }
            log.info("预聚合快照完成 statDate={} rows={}", statDate, agg.size());
        } catch (Exception e) {
            log.error("预聚合快照失败 statDate={}", statDate, e);
        }
    }
}
