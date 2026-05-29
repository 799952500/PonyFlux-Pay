package com.payflow.admin.service.recon;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.admin.dto.recon.ReconDiffAssignRequest;
import com.payflow.admin.dto.recon.ReconDiffCommentRequest;
import com.payflow.admin.dto.recon.ReconDiffCompleteRequest;
import com.payflow.admin.dto.recon.ReconDiffStartRequest;
import com.payflow.admin.entity.recon.ReconDiffAssignmentEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.enums.NotificationTypeEnum;
import com.payflow.admin.enums.recon.ReconDiffWorkflowStatusEnum;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.service.NotificationService;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 对账差异工单工作流编排。
 */
@Service
@RequiredArgsConstructor
public class ReconDiffWorkflowService {

    private final ReconDiffEntityMapper reconDiffEntityMapper;
    private final ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper;
    private final ReconAuditService reconAuditService;
    private final NotificationService notificationService;
    private final ReconSlaService reconSlaService;

    @Transactional
    public void ensureWorkItemExists(ReconDiffEntity diff) {
        ReconDiffAssignmentEntity existing = reconDiffAssignmentEntityMapper.selectOne(
                Wrappers.<ReconDiffAssignmentEntity>lambdaQuery()
                        .eq(ReconDiffAssignmentEntity::getDiffId, diff.getId()));
        if (existing != null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        ReconDiffAssignmentEntity a = new ReconDiffAssignmentEntity();
        a.setDiffId(diff.getId());
        a.setMerchantId(StringUtils.hasText(diff.getMerchantId()) ? diff.getMerchantId() : "UNKNOWN");
        a.setWorkflowStatus(ReconDiffWorkflowStatusEnum.UNASSIGNED.name());
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        reconSlaService.computeDueAt(diff.getDiffType(), now).ifPresent(a::setDueAt);
        reconDiffAssignmentEntityMapper.insert(a);
    }

    @Transactional
    public void claim(long diffId, String operator, String clientIp) {
        ReconDiffEntity diff = mustGetDiff(diffId);
        ReconDiffAssignmentEntity a = mustGetAssignment(diffId);
        if (!ReconDiffWorkflowStatusEnum.UNASSIGNED.name().equals(a.getWorkflowStatus())) {
            throw new BizException(7561, "仅未指派工单可认领");
        }
        if (StringUtils.hasText(a.getAssigneeId())) {
            throw new BizException(7562, "该差异已被认领: " + a.getAssigneeId());
        }
        LocalDateTime now = LocalDateTime.now();
        a.setAssigneeId(operator);
        a.setWorkflowStatus(ReconDiffWorkflowStatusEnum.ASSIGNED.name());
        a.setAssignedAt(now);
        if (a.getDueAt() == null) {
            reconSlaService.computeDueAt(diff.getDiffType(), now).ifPresent(a::setDueAt);
        }
        a.setUpdatedAt(now);
        reconDiffAssignmentEntityMapper.updateById(a);
        reconAuditService.record(diffId, "ASSIGN", operator, "认领工单", clientIp);
        syncDiffHandleStatusIfNeeded(diff, "PENDING");
    }

    @Transactional
    public void assign(long diffId, ReconDiffAssignRequest request, String operator, String clientIp) {
        ReconDiffEntity diff = mustGetDiff(diffId);
        ReconDiffAssignmentEntity a = mustGetAssignment(diffId);
        String newAssignee = request.getAssigneeId().trim();
        String old = a.getAssigneeId();
        LocalDateTime now = LocalDateTime.now();
        a.setAssigneeId(newAssignee);
        a.setWorkflowStatus(ReconDiffWorkflowStatusEnum.ASSIGNED.name());
        a.setAssignedAt(now);
        if (a.getDueAt() == null) {
            reconSlaService.computeDueAt(diff.getDiffType(), now).ifPresent(a::setDueAt);
        }
        a.setUpdatedAt(now);
        reconDiffAssignmentEntityMapper.updateById(a);
        String detail = String.format("指派: %s -> %s%s",
                StringUtils.hasText(old) ? old : "UNASSIGNED",
                newAssignee,
                StringUtils.hasText(request.getRemark()) ? ("; " + request.getRemark()) : "");
        reconAuditService.record(diffId, "REASSIGN", operator, detail, clientIp);

        try {
            notificationService.sendToRole(
                    NotificationTypeEnum.RECON_DIFF_ASSIGNED,
                    String.valueOf(diffId),
                    "对账差异工单指派",
                    "对账差异工单已指派给 " + newAssignee,
                    "/admin/reconcile",
                    diff.getMerchantId(),
                    "recon:diff:handle");
        } catch (Exception ignored) {
            // 通知失败不阻断核心流程
        }
        syncDiffHandleStatusIfNeeded(diff, "PENDING");
    }

    @Transactional
    public void start(long diffId, ReconDiffStartRequest request, String operator, String clientIp) {
        ReconDiffEntity diff = mustGetDiff(diffId);
        ReconDiffAssignmentEntity a = mustGetAssignment(diffId);
        mustBeAssigneeOrThrow(a, operator);
        if (!isIn(a.getWorkflowStatus(), ReconDiffWorkflowStatusEnum.ASSIGNED, ReconDiffWorkflowStatusEnum.ESCALATED)) {
            throw new BizException(7563, "当前状态不允许开始处理: " + a.getWorkflowStatus());
        }
        LocalDateTime now = LocalDateTime.now();
        a.setWorkflowStatus(ReconDiffWorkflowStatusEnum.IN_PROGRESS.name());
        a.setUpdatedAt(now);
        reconDiffAssignmentEntityMapper.updateById(a);
        reconAuditService.record(diffId, "START_PROGRESS",
                operator,
                StringUtils.hasText(request.getRemark()) ? request.getRemark() : "开始处理",
                clientIp);
        syncDiffHandleStatusIfNeeded(diff, "PENDING");
    }

    @Transactional
    public void complete(long diffId, ReconDiffCompleteRequest request, String operator, String clientIp) {
        ReconDiffEntity diff = mustGetDiff(diffId);
        ReconDiffAssignmentEntity a = mustGetAssignment(diffId);
        mustBeAssigneeOrThrow(a, operator);
        completeInternal(diffId, request, operator, clientIp, true);
    }

    private void completeInternal(
            long diffId,
            ReconDiffCompleteRequest request,
            String operator,
            String clientIp,
            boolean syncDiffHandle) {
        ReconDiffEntity diff = mustGetDiff(diffId);
        ReconDiffAssignmentEntity a = mustGetAssignment(diffId);

        String action = request.getAction().trim().toUpperCase();
        if (!Objects.equals(action, "PROCESSED")
                && !Objects.equals(action, "IGNORED")
                && !Objects.equals(action, ReconDiffWorkflowStatusEnum.ACCEPTED_LOSS.name())) {
            throw new BizException(7564, "action 不合法: " + action);
        }
        if (!StringUtils.hasText(request.getRemark())) {
            throw new BizException(7565, "remark 不能为空");
        }
        if (Objects.equals(action, "PROCESSED") || Objects.equals(action, "IGNORED")) {
            if (request.getRemark().trim().length() < 10) {
                throw new BizException(7566, "处置说明至少 10 个字符");
            }
        }
        if (Objects.equals(action, ReconDiffWorkflowStatusEnum.ACCEPTED_LOSS.name())) {
            if (request.getRemark().trim().length() < 20) {
                throw new BizException(7567, "挂账原因至少 20 个字符");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        String next = action;
        if (Objects.equals(action, ReconDiffWorkflowStatusEnum.ACCEPTED_LOSS.name())) {
            next = ReconDiffWorkflowStatusEnum.ACCEPTED_LOSS.name();
        }
        a.setWorkflowStatus(next);
        a.setProcessedAt(now);
        a.setUpdatedAt(now);
        reconDiffAssignmentEntityMapper.updateById(a);
        reconAuditService.record(diffId, "COMPLETE", operator, action + ": " + request.getRemark(), clientIp);

        if (syncDiffHandle && (Objects.equals(action, "PROCESSED") || Objects.equals(action, "IGNORED"))) {
            diff.setHandleStatus(action);
            diff.setHandleRemark(request.getRemark());
            diff.setHandledBy(operator);
            diff.setHandledAt(now);
            reconDiffEntityMapper.updateById(diff);
        }
    }

    /**
     * 管理端批量挂账：不校验负责人，仅允许 ACCEPTED_LOSS。
     */
    @Transactional
    public void completeAsManager(long diffId, ReconDiffCompleteRequest request, String operator, String clientIp) {
        if (!ReconDiffWorkflowStatusEnum.ACCEPTED_LOSS.name().equals(request.getAction().trim().toUpperCase())) {
            throw new BizException(7569, "管理端批量操作仅支持 ACCEPTED_LOSS");
        }
        completeInternal(diffId, request, operator, clientIp, false);
    }

    @Transactional
    public void comment(long diffId, ReconDiffCommentRequest request, String operator, String clientIp) {
        mustGetDiff(diffId);
        ReconDiffAssignmentEntity a = mustGetAssignment(diffId);
        mustBeAssigneeOrThrow(a, operator);
        reconAuditService.record(diffId, "COMMENT", operator, request.getContent(), clientIp);
    }

    private ReconDiffEntity mustGetDiff(long diffId) {
        ReconDiffEntity diff = reconDiffEntityMapper.selectById(diffId);
        if (diff == null) {
            throw new BizException(7543, "差异记录不存在: " + diffId);
        }
        return diff;
    }

    private ReconDiffAssignmentEntity mustGetAssignment(long diffId) {
        ReconDiffAssignmentEntity a = reconDiffAssignmentEntityMapper.selectOne(
                Wrappers.<ReconDiffAssignmentEntity>lambdaQuery().eq(ReconDiffAssignmentEntity::getDiffId, diffId));
        if (a == null) {
            throw new BizException(7560, "工单不存在: " + diffId);
        }
        return a;
    }

    private static void mustBeAssigneeOrThrow(ReconDiffAssignmentEntity a, String operator) {
        if (!StringUtils.hasText(a.getAssigneeId()) || !a.getAssigneeId().equals(operator)) {
            throw new BizException(7568, "仅负责人可操作该工单");
        }
    }

    private static boolean isIn(String status, ReconDiffWorkflowStatusEnum... allowed) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        for (ReconDiffWorkflowStatusEnum a : allowed) {
            if (a.name().equals(status)) {
                return true;
            }
        }
        return false;
    }

    private void syncDiffHandleStatusIfNeeded(ReconDiffEntity diff, String status) {
        if (!StringUtils.hasText(diff.getHandleStatus()) || !diff.getHandleStatus().equals(status)) {
            diff.setHandleStatus(status);
            reconDiffEntityMapper.updateById(diff);
        }
    }
}

