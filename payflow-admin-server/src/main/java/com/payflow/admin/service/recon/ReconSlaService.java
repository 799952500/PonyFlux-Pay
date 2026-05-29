package com.payflow.admin.service.recon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.admin.dto.recon.ReconDiffSlaRuleDTO;
import com.payflow.admin.dto.recon.ReconDiffSlaRuleUpsertRequest;
import com.payflow.admin.entity.recon.ReconDiffAssignmentEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconDiffSlaRuleEntity;
import com.payflow.admin.enums.NotificationTypeEnum;
import com.payflow.admin.enums.recon.ReconDiffWorkflowStatusEnum;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffSlaRuleEntityMapper;
import com.payflow.admin.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * SLA 规则管理（不溯及既往：仅影响新工单的 dueAt 写入）。
 */
@Slf4j
@Service
public class ReconSlaService {

    private static final Set<String> OPEN_STATUSES = Set.of(
            ReconDiffWorkflowStatusEnum.UNASSIGNED.name(),
            ReconDiffWorkflowStatusEnum.ASSIGNED.name(),
            ReconDiffWorkflowStatusEnum.IN_PROGRESS.name(),
            ReconDiffWorkflowStatusEnum.ESCALATED.name());

    private final ReconDiffSlaRuleEntityMapper reconDiffSlaRuleEntityMapper;
    private final ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper;
    private final ReconDiffEntityMapper reconDiffEntityMapper;
    private final NotificationService notificationService;
    private final ReconAuditService reconAuditService;

    public ReconSlaService(
            ReconDiffSlaRuleEntityMapper reconDiffSlaRuleEntityMapper,
            ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper,
            ReconDiffEntityMapper reconDiffEntityMapper,
            NotificationService notificationService,
            ReconAuditService reconAuditService) {
        this.reconDiffSlaRuleEntityMapper = reconDiffSlaRuleEntityMapper;
        this.reconDiffAssignmentEntityMapper = reconDiffAssignmentEntityMapper;
        this.reconDiffEntityMapper = reconDiffEntityMapper;
        this.notificationService = notificationService;
        this.reconAuditService = reconAuditService;
    }

    public List<ReconDiffSlaRuleDTO> listRules() {
        List<ReconDiffSlaRuleEntity> rows = reconDiffSlaRuleEntityMapper.selectList(
                new LambdaQueryWrapper<ReconDiffSlaRuleEntity>().orderByAsc(ReconDiffSlaRuleEntity::getDiffType));
        return rows.stream().map(ReconSlaService::toDto).toList();
    }

    public Optional<ReconDiffSlaRuleEntity> getEnabledRule(String diffType) {
        ReconDiffSlaRuleEntity row = reconDiffSlaRuleEntityMapper.selectOne(
                new LambdaQueryWrapper<ReconDiffSlaRuleEntity>()
                        .eq(ReconDiffSlaRuleEntity::getDiffType, diffType)
                        .eq(ReconDiffSlaRuleEntity::getEnabled, 1));
        return Optional.ofNullable(row);
    }

    public Optional<LocalDateTime> computeDueAt(String diffType, LocalDateTime baseTime) {
        Optional<ReconDiffSlaRuleEntity> rule = getEnabledRule(diffType);
        if (rule.isEmpty()) {
            return Optional.empty();
        }
        Integer hours = rule.get().getSlaHours();
        if (hours == null || hours <= 0) {
            return Optional.empty();
        }
        return Optional.of(baseTime.plusHours(hours));
    }

    public boolean isDueSoon(LocalDateTime now, LocalDateTime createdAt, LocalDateTime dueAt, BigDecimal dueSoonRatio) {
        if (dueAt == null || createdAt == null || dueSoonRatio == null) {
            return false;
        }
        long totalSeconds = java.time.Duration.between(createdAt, dueAt).getSeconds();
        if (totalSeconds <= 0) {
            return false;
        }
        long remainingSeconds = java.time.Duration.between(now, dueAt).getSeconds();
        if (remainingSeconds <= 0) {
            return false;
        }
        BigDecimal remainRatio = BigDecimal.valueOf(remainingSeconds)
                .divide(BigDecimal.valueOf(totalSeconds), 6, java.math.RoundingMode.HALF_UP);
        return remainRatio.compareTo(dueSoonRatio) <= 0;
    }

    @Transactional
    public void upsert(String diffType, ReconDiffSlaRuleUpsertRequest req, String operator) {
        ReconDiffSlaRuleEntity existing = reconDiffSlaRuleEntityMapper.selectOne(
                new LambdaQueryWrapper<ReconDiffSlaRuleEntity>().eq(ReconDiffSlaRuleEntity::getDiffType, diffType));
        LocalDateTime now = LocalDateTime.now();

        ReconDiffSlaRuleEntity row = existing != null ? existing : new ReconDiffSlaRuleEntity();
        row.setDiffType(diffType);
        row.setEnabled(Boolean.TRUE.equals(req.getEnabled()) ? 1 : 0);
        row.setSlaHours(req.getSlaHours());
        row.setDueSoonRatio(req.getDueSoonRatio());
        row.setEscalateToRole(req.getEscalateToRole());
        row.setUpdatedBy(operator);
        row.setUpdatedAt(now);

        if (existing == null) {
            reconDiffSlaRuleEntityMapper.insert(row);
        } else {
            reconDiffSlaRuleEntityMapper.updateById(row);
        }
    }

    /**
     * SLA 扫描：due-soon 提醒（去重）+ overdue 自动升级（仅一次）。
     *
     * @return [dueSoonCount, overdueEscalatedCount]
     */
    @Transactional
    public int[] scanDueSoonAndOverdue() {
        LocalDateTime now = LocalDateTime.now();
        List<ReconDiffAssignmentEntity> open = reconDiffAssignmentEntityMapper.selectList(
                Wrappers.<ReconDiffAssignmentEntity>lambdaQuery()
                        .in(ReconDiffAssignmentEntity::getWorkflowStatus, OPEN_STATUSES)
                        .isNotNull(ReconDiffAssignmentEntity::getDueAt));
        if (open.isEmpty()) {
            return new int[]{0, 0};
        }
        Map<Long, ReconDiffEntity> diffById = reconDiffEntityMapper.selectBatchIds(
                        open.stream().map(ReconDiffAssignmentEntity::getDiffId).toList())
                .stream()
                .collect(Collectors.toMap(ReconDiffEntity::getId, d -> d, (a, b) -> a));
        Map<String, ReconDiffSlaRuleEntity> ruleByType = reconDiffSlaRuleEntityMapper.selectList(
                        new LambdaQueryWrapper<ReconDiffSlaRuleEntity>().eq(ReconDiffSlaRuleEntity::getEnabled, 1))
                .stream()
                .collect(Collectors.toMap(ReconDiffSlaRuleEntity::getDiffType, r -> r, (a, b) -> a));

        int dueSoonCount = 0;
        int overdueCount = 0;
        for (ReconDiffAssignmentEntity a : open) {
            ReconDiffEntity diff = diffById.get(a.getDiffId());
            if (diff == null) {
                continue;
            }
            ReconDiffSlaRuleEntity rule = ruleByType.get(diff.getDiffType());
            if (rule == null) {
                continue;
            }
            BigDecimal ratio = rule.getDueSoonRatio() != null ? rule.getDueSoonRatio() : new BigDecimal("0.2");
            if (a.getDueAt().isBefore(now)) {
                if (a.getEscalatedAt() == null) {
                    escalateOverdue(a, diff, rule, now);
                    overdueCount++;
                }
            } else if (isDueSoon(now, a.getCreatedAt(), a.getDueAt(), ratio)) {
                if (shouldRemindDueSoon(a, now)) {
                    notifyDueSoon(a, diff);
                    a.setLastRemindedAt(now);
                    a.setUpdatedAt(now);
                    reconDiffAssignmentEntityMapper.updateById(a);
                    dueSoonCount++;
                }
            }
        }
        return new int[]{dueSoonCount, overdueCount};
    }

    private static boolean shouldRemindDueSoon(ReconDiffAssignmentEntity a, LocalDateTime now) {
        if (a.getLastRemindedAt() == null) {
            return true;
        }
        return a.getLastRemindedAt().isBefore(now.minusHours(4));
    }

    private void notifyDueSoon(ReconDiffAssignmentEntity a, ReconDiffEntity diff) {
        String bizKey = "due-soon-" + a.getDiffId();
        try {
            notificationService.sendToRole(
                    NotificationTypeEnum.RECON_DIFF_DUE_SOON,
                    bizKey,
                    "对账差异即将超时",
                    "差异 " + a.getDiffId() + " 将于 " + a.getDueAt() + " 到期",
                    "/admin/reconcile/work-items/" + a.getDiffId(),
                    diff.getMerchantId(),
                    "recon:diff:handle");
        } catch (Exception e) {
            log.warn("发送 due-soon 通知失败 diffId={}", a.getDiffId(), e);
        }
    }

    private void escalateOverdue(
            ReconDiffAssignmentEntity a,
            ReconDiffEntity diff,
            ReconDiffSlaRuleEntity rule,
            LocalDateTime now) {
        a.setWorkflowStatus(ReconDiffWorkflowStatusEnum.ESCALATED.name());
        a.setEscalatedAt(now);
        a.setEscalatedToRole(StringUtils.hasText(rule.getEscalateToRole()) ? rule.getEscalateToRole() : "recon:manage");
        a.setUpdatedAt(now);
        reconDiffAssignmentEntityMapper.updateById(a);
        reconAuditService.record(a.getDiffId(), "ESCALATE", "system", "SLA 超时自动升级", null);
        String perm = a.getEscalatedToRole();
        try {
            notificationService.sendToRole(
                    NotificationTypeEnum.RECON_DIFF_OVERDUE,
                    "overdue-" + a.getDiffId(),
                    "对账差异 SLA 超时",
                    "差异 " + a.getDiffId() + " 已超时并升级，请尽快处理",
                    "/admin/reconcile/work-items/" + a.getDiffId(),
                    diff.getMerchantId(),
                    perm);
        } catch (Exception e) {
            log.warn("发送 overdue 通知失败 diffId={}", a.getDiffId(), e);
        }
    }

    private static ReconDiffSlaRuleDTO toDto(ReconDiffSlaRuleEntity row) {
        ReconDiffSlaRuleDTO dto = new ReconDiffSlaRuleDTO();
        dto.setDiffType(row.getDiffType());
        dto.setEnabled(row.getEnabled() != null && row.getEnabled() == 1);
        dto.setSlaHours(row.getSlaHours());
        dto.setDueSoonRatio(row.getDueSoonRatio());
        dto.setEscalateToRole(row.getEscalateToRole());
        dto.setUpdatedBy(row.getUpdatedBy());
        dto.setUpdatedAt(row.getUpdatedAt());
        return dto;
    }
}

