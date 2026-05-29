package com.payflow.admin.service.recon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.admin.dto.recon.ReconDiffAggregationDTO;
import com.payflow.admin.dto.recon.ReconReportSubscribeRequest;
import com.payflow.admin.dto.recon.ReconReportSubscriptionDTO;
import com.payflow.admin.entity.recon.ReconReportSnapshotEntity;
import com.payflow.admin.entity.recon.ReconReportSubscriptionEntity;
import com.payflow.admin.enums.NotificationTypeEnum;
import com.payflow.admin.entity.SysUser;
import com.payflow.admin.mapper.SysUserMapper;
import com.payflow.admin.mapper.recon.ReconReportSnapshotEntityMapper;
import com.payflow.admin.mapper.recon.ReconReportSubscriptionEntityMapper;
import com.payflow.admin.service.NotificationService;
import com.payflow.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对账报告订阅与快照生成。
 */
@Service
public class ReconReportService {

    private final ReconReportSubscriptionEntityMapper subscriptionMapper;
    private final ReconReportSnapshotEntityMapper snapshotMapper;
    private final ReconAggregationService reconAggregationService;
    private final NotificationService notificationService;
    private final SysUserMapper sysUserMapper;
    private final ObjectMapper objectMapper;

    public ReconReportService(
            ReconReportSubscriptionEntityMapper subscriptionMapper,
            ReconReportSnapshotEntityMapper snapshotMapper,
            ReconAggregationService reconAggregationService,
            NotificationService notificationService,
            SysUserMapper sysUserMapper,
            ObjectMapper objectMapper) {
        this.subscriptionMapper = subscriptionMapper;
        this.snapshotMapper = snapshotMapper;
        this.reconAggregationService = reconAggregationService;
        this.notificationService = notificationService;
        this.sysUserMapper = sysUserMapper;
        this.objectMapper = objectMapper;
    }

    public List<ReconReportSubscriptionDTO> listBySubscriber(String subscriberId) {
        return subscriptionMapper.selectList(
                        Wrappers.<ReconReportSubscriptionEntity>lambdaQuery()
                                .eq(ReconReportSubscriptionEntity::getSubscriberId, subscriberId)
                                .orderByDesc(ReconReportSubscriptionEntity::getUpdatedAt))
                .stream()
                .map(ReconReportService::toDto)
                .toList();
    }

    @Transactional
    public ReconReportSubscriptionDTO subscribe(String subscriberId, ReconReportSubscribeRequest req) {
        String type = req.getReportType().trim().toUpperCase();
        ReconReportSubscriptionEntity existing = subscriptionMapper.selectOne(
                Wrappers.<ReconReportSubscriptionEntity>lambdaQuery()
                        .eq(ReconReportSubscriptionEntity::getSubscriberId, subscriberId)
                        .eq(ReconReportSubscriptionEntity::getReportType, type));
        LocalDateTime now = LocalDateTime.now();
        ReconReportSubscriptionEntity row = existing != null ? existing : new ReconReportSubscriptionEntity();
        row.setSubscriberId(subscriberId);
        row.setReportType(type);
        row.setScope(req.getScope().trim().toUpperCase());
        row.setEnabled(Boolean.FALSE.equals(req.getEnabled()) ? 0 : 1);
        row.setUpdatedAt(now);
        if (existing == null) {
            row.setCreatedAt(now);
            subscriptionMapper.insert(row);
        } else {
            subscriptionMapper.updateById(row);
        }
        return toDto(row);
    }

    @Transactional
    public void unsubscribe(String subscriberId, long id) {
        ReconReportSubscriptionEntity row = subscriptionMapper.selectById(id);
        if (row == null || !subscriberId.equals(row.getSubscriberId())) {
            throw new BizException(7571, "订阅不存在或无权操作");
        }
        subscriptionMapper.deleteById(id);
    }

    public Map<String, Object> getReportSnapshot(String snapshotId, String requester) {
        ReconReportSnapshotEntity snap = snapshotMapper.selectOne(
                Wrappers.<ReconReportSnapshotEntity>lambdaQuery()
                        .eq(ReconReportSnapshotEntity::getSnapshotId, snapshotId));
        if (snap == null) {
            throw new BizException(7572, "报告不存在: " + snapshotId);
        }
        if (!requester.equals(snap.getSubscriberId()) && !"admin".equals(requester)) {
            throw new BizException(7573, "无权查看该报告");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("snapshotId", snap.getSnapshotId());
        data.put("reportType", snap.getReportType());
        data.put("periodStart", snap.getPeriodStart());
        data.put("periodEnd", snap.getPeriodEnd());
        data.put("generatedAt", snap.getGeneratedAt());
        try {
            data.put("payload", objectMapper.readValue(snap.getPayloadJson(), Map.class));
        } catch (JsonProcessingException e) {
            data.put("payload", Map.of());
        }
        return data;
    }

    @Transactional
    public int generateAndNotify(String reportType) {
        String type = reportType.trim().toUpperCase();
        List<ReconReportSubscriptionEntity> subs = subscriptionMapper.selectList(
                new LambdaQueryWrapper<ReconReportSubscriptionEntity>()
                        .eq(ReconReportSubscriptionEntity::getReportType, type)
                        .eq(ReconReportSubscriptionEntity::getEnabled, 1));
        int count = 0;
        LocalDate today = LocalDate.now();
        LocalDate dateFrom = "WEEKLY".equals(type) ? today.minusDays(7) : today.minusDays(1);
        LocalDate dateTo = today.minusDays(1);
        for (ReconReportSubscriptionEntity sub : subs) {
            try {
                generateOne(sub, dateFrom, dateTo);
                count++;
            } catch (Exception e) {
                // 单条失败不阻断
            }
        }
        return count;
    }

    private void generateOne(ReconReportSubscriptionEntity sub, LocalDate dateFrom, LocalDate dateTo)
            throws JsonProcessingException {
        List<String> scope = "ALL_AUTHORIZED".equals(sub.getScope()) ? null : List.of();
        ReconDiffAggregationDTO dashboard = reconAggregationService.buildDashboard(
                dateFrom, dateTo, null, null, scope);
        String snapshotId = "RPT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        LocalDateTime now = LocalDateTime.now();
        ReconReportSnapshotEntity snap = new ReconReportSnapshotEntity();
        snap.setSnapshotId(snapshotId);
        snap.setSubscriberId(sub.getSubscriberId());
        snap.setReportType(sub.getReportType());
        snap.setPeriodStart(dateFrom.atStartOfDay());
        snap.setPeriodEnd(dateTo.atTime(23, 59, 59));
        snap.setPayloadJson(objectMapper.writeValueAsString(dashboard));
        snap.setGeneratedAt(now);
        snapshotMapper.insert(snap);

        sub.setLastSentAt(now);
        sub.setUpdatedAt(now);
        subscriptionMapper.updateById(sub);

        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, sub.getSubscriberId()));
        if (user != null && user.getId() != null) {
            notificationService.send(
                    NotificationTypeEnum.RECON_REPORT,
                    snapshotId,
                    "对账" + ("WEEKLY".equals(sub.getReportType()) ? "周报" : "日报") + "已生成",
                    "报告周期 " + dateFrom + " ~ " + dateTo,
                    "/admin/reconcile/reports/" + snapshotId,
                    null,
                    List.of(user.getId()));
        }
    }

    private static ReconReportSubscriptionDTO toDto(ReconReportSubscriptionEntity row) {
        ReconReportSubscriptionDTO dto = new ReconReportSubscriptionDTO();
        dto.setId(row.getId());
        dto.setSubscriberId(row.getSubscriberId());
        dto.setReportType(row.getReportType());
        dto.setScope(row.getScope());
        dto.setEnabled(row.getEnabled() != null && row.getEnabled() == 1);
        dto.setLastSentAt(row.getLastSentAt());
        dto.setCreatedAt(row.getCreatedAt());
        return dto;
    }
}
