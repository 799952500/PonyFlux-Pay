package com.payflow.admin.service.recon;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.admin.dto.recon.ReconDiffAggregationDTO;
import com.payflow.admin.entity.recon.ReconDiffAssignmentEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.enums.recon.ReconDiffWorkflowStatusEnum;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 差异归因看板聚合（在线计算，数据量大时可启用预聚合快照表）。
 */
@Service
public class ReconAggregationService {

    private final ReconTaskEntityMapper reconTaskEntityMapper;
    private final ReconDiffEntityMapper reconDiffEntityMapper;
    private final ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper;

    public ReconAggregationService(
            ReconTaskEntityMapper reconTaskEntityMapper,
            ReconDiffEntityMapper reconDiffEntityMapper,
            ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper) {
        this.reconTaskEntityMapper = reconTaskEntityMapper;
        this.reconDiffEntityMapper = reconDiffEntityMapper;
        this.reconDiffAssignmentEntityMapper = reconDiffAssignmentEntityMapper;
    }

    public ReconDiffAggregationDTO buildDashboard(
            LocalDate dateFrom,
            LocalDate dateTo,
            String channel,
            String diffType,
            List<String> merchantScopeIds) {
        List<ReconTaskEntity> tasks = reconTaskEntityMapper.selectList(
                Wrappers.<ReconTaskEntity>lambdaQuery()
                        .ge(ReconTaskEntity::getBillDate, dateFrom)
                        .le(ReconTaskEntity::getBillDate, dateTo)
                        .eq(StringUtils.hasText(channel), ReconTaskEntity::getChannel, channel));
        Map<String, ReconTaskEntity> taskById = tasks.stream()
                .collect(Collectors.toMap(ReconTaskEntity::getTaskId, t -> t, (a, b) -> a));
        if (taskById.isEmpty()) {
            return emptyDashboard();
        }
        List<ReconDiffEntity> diffs = reconDiffEntityMapper.selectList(
                Wrappers.<ReconDiffEntity>lambdaQuery().in(ReconDiffEntity::getTaskId, taskById.keySet()));
        if (StringUtils.hasText(diffType)) {
            diffs = diffs.stream()
                    .filter(d -> diffType.equalsIgnoreCase(d.getDiffType()))
                    .toList();
        }
        Map<Long, ReconDiffAssignmentEntity> assignmentByDiff = reconDiffAssignmentEntityMapper.selectList(
                        Wrappers.<ReconDiffAssignmentEntity>lambdaQuery()
                                .in(ReconDiffAssignmentEntity::getDiffId,
                                        diffs.stream().map(ReconDiffEntity::getId).toList()))
                .stream()
                .collect(Collectors.toMap(ReconDiffAssignmentEntity::getDiffId, a -> a, (x, y) -> x));

        List<ReconDiffEntity> scoped = new ArrayList<>();
        for (ReconDiffEntity d : diffs) {
            String merchantId = resolveMerchantId(d, assignmentByDiff.get(d.getId()));
            if (!isMerchantAllowed(merchantId, merchantScopeIds)) {
                continue;
            }
            scoped.add(d);
        }

        ReconDiffAggregationDTO dto = new ReconDiffAggregationDTO();
        dto.setMatrix(buildMatrix(scoped, taskById));
        dto.setTrend(buildTrend(scoped, taskById));
        dto.setTopMerchants(buildTopMerchants(scoped, assignmentByDiff));
        dto.setTopAccounts(buildTopAccounts(scoped, taskById));
        dto.setSlaStats(buildSlaStats(scoped, assignmentByDiff));
        return dto;
    }

    private static ReconDiffAggregationDTO emptyDashboard() {
        ReconDiffAggregationDTO dto = new ReconDiffAggregationDTO();
        dto.setSlaStats(new ReconDiffAggregationDTO.SlaStats());
        return dto;
    }

    private static String resolveMerchantId(ReconDiffEntity d, ReconDiffAssignmentEntity a) {
        if (a != null && StringUtils.hasText(a.getMerchantId())) {
            return a.getMerchantId();
        }
        return d.getMerchantId();
    }

    private static boolean isMerchantAllowed(String merchantId, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return true;
        }
        return StringUtils.hasText(merchantId) && merchantScopeIds.contains(merchantId);
    }

    private static long diffAmount(ReconDiffEntity d) {
        long ch = d.getChannelAmount() != null ? d.getChannelAmount() : 0L;
        long loc = d.getLocalAmount() != null ? d.getLocalAmount() : 0L;
        return Math.max(ch, loc);
    }

    private List<ReconDiffAggregationDTO.MatrixCell> buildMatrix(
            List<ReconDiffEntity> diffs,
            Map<String, ReconTaskEntity> taskById) {
        Map<String, ReconDiffAggregationDTO.MatrixCell> cells = new HashMap<>();
        for (ReconDiffEntity d : diffs) {
            ReconTaskEntity t = taskById.get(d.getTaskId());
            String ch = t != null ? t.getChannel() : "unknown";
            String key = ch + "|" + d.getDiffType();
            ReconDiffAggregationDTO.MatrixCell cell = cells.computeIfAbsent(key, k -> {
                ReconDiffAggregationDTO.MatrixCell c = new ReconDiffAggregationDTO.MatrixCell();
                c.setChannel(ch);
                c.setDiffType(d.getDiffType());
                return c;
            });
            cell.setDiffCount(cell.getDiffCount() + 1);
            cell.setDiffAmount(cell.getDiffAmount() + diffAmount(d));
        }
        return cells.values().stream()
                .sorted(Comparator.comparing(ReconDiffAggregationDTO.MatrixCell::getChannel)
                        .thenComparing(ReconDiffAggregationDTO.MatrixCell::getDiffType))
                .toList();
    }

    private List<ReconDiffAggregationDTO.TrendPoint> buildTrend(
            List<ReconDiffEntity> diffs,
            Map<String, ReconTaskEntity> taskById) {
        Map<String, ReconDiffAggregationDTO.TrendPoint> byDay = new HashMap<>();
        for (ReconDiffEntity d : diffs) {
            ReconTaskEntity t = taskById.get(d.getTaskId());
            String period = t != null && t.getBillDate() != null ? t.getBillDate().toString() : "unknown";
            ReconDiffAggregationDTO.TrendPoint p = byDay.computeIfAbsent(period, k -> {
                ReconDiffAggregationDTO.TrendPoint tp = new ReconDiffAggregationDTO.TrendPoint();
                tp.setPeriod(period);
                return tp;
            });
            p.setDiffCount(p.getDiffCount() + 1);
            p.setDiffAmount(p.getDiffAmount() + diffAmount(d));
        }
        return byDay.values().stream()
                .sorted(Comparator.comparing(ReconDiffAggregationDTO.TrendPoint::getPeriod))
                .toList();
    }

    private List<ReconDiffAggregationDTO.TopItem> buildTopMerchants(
            List<ReconDiffEntity> diffs,
            Map<Long, ReconDiffAssignmentEntity> assignmentByDiff) {
        Map<String, ReconDiffAggregationDTO.TopItem> map = new HashMap<>();
        for (ReconDiffEntity d : diffs) {
            String mid = resolveMerchantId(d, assignmentByDiff.get(d.getId()));
            ReconDiffAggregationDTO.TopItem item = map.computeIfAbsent(mid, k -> {
                ReconDiffAggregationDTO.TopItem t = new ReconDiffAggregationDTO.TopItem();
                t.setKey(mid);
                return t;
            });
            item.setDiffCount(item.getDiffCount() + 1);
            item.setDiffAmount(item.getDiffAmount() + diffAmount(d));
        }
        return map.values().stream()
                .sorted(Comparator.comparingLong(ReconDiffAggregationDTO.TopItem::getDiffCount).reversed())
                .limit(10)
                .toList();
    }

    private List<ReconDiffAggregationDTO.TopItem> buildTopAccounts(
            List<ReconDiffEntity> diffs,
            Map<String, ReconTaskEntity> taskById) {
        Map<String, ReconDiffAggregationDTO.TopItem> map = new HashMap<>();
        for (ReconDiffEntity d : diffs) {
            ReconTaskEntity t = taskById.get(d.getTaskId());
            String acct = t != null ? t.getAccountCode() : "unknown";
            ReconDiffAggregationDTO.TopItem item = map.computeIfAbsent(acct, k -> {
                ReconDiffAggregationDTO.TopItem ti = new ReconDiffAggregationDTO.TopItem();
                ti.setKey(acct);
                return ti;
            });
            item.setDiffCount(item.getDiffCount() + 1);
            item.setDiffAmount(item.getDiffAmount() + diffAmount(d));
        }
        return map.values().stream()
                .sorted(Comparator.comparingLong(ReconDiffAggregationDTO.TopItem::getDiffCount).reversed())
                .limit(10)
                .toList();
    }

    private ReconDiffAggregationDTO.SlaStats buildSlaStats(
            List<ReconDiffEntity> diffs,
            Map<Long, ReconDiffAssignmentEntity> assignmentByDiff) {
        ReconDiffAggregationDTO.SlaStats stats = new ReconDiffAggregationDTO.SlaStats();
        long sample = 0;
        long met = 0;
        long longTail = 0;
        double totalMinutes = 0;
        LocalDate today = LocalDate.now();
        for (ReconDiffEntity d : diffs) {
            ReconDiffAssignmentEntity a = assignmentByDiff.get(d.getId());
            if (a == null) {
                continue;
            }
            if (isTerminal(a.getWorkflowStatus())) {
                sample++;
                if (a.getProcessedAt() != null && a.getCreatedAt() != null) {
                    totalMinutes += ChronoUnit.MINUTES.between(a.getCreatedAt(), a.getProcessedAt());
                }
                if (a.getDueAt() != null && a.getProcessedAt() != null && !a.getProcessedAt().isAfter(a.getDueAt())) {
                    met++;
                }
            } else if (a.getCreatedAt() != null) {
                long ageDays = ChronoUnit.DAYS.between(a.getCreatedAt().toLocalDate(), today);
                if (ageDays >= 7) {
                    longTail++;
                }
            }
        }
        stats.setSample(sample);
        stats.setAvgHandleMinutes(sample > 0 ? totalMinutes / sample : null);
        stats.setSlaMetRate(sample > 0 ? (double) met / sample : null);
        long openCount = diffs.size() - sample;
        stats.setLongTailRate(diffs.isEmpty() ? null : (double) longTail / Math.max(1, openCount + longTail));
        return stats;
    }

    private static boolean isTerminal(String status) {
        return ReconDiffWorkflowStatusEnum.PROCESSED.name().equals(status)
                || ReconDiffWorkflowStatusEnum.IGNORED.name().equals(status)
                || ReconDiffWorkflowStatusEnum.ACCEPTED_LOSS.name().equals(status);
    }
}
