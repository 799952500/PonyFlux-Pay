package com.payflow.admin.controller;

import com.payflow.admin.dto.HandleReconDiffRequest;
import com.payflow.admin.dto.ManualReconRequest;
import com.payflow.admin.dto.recon.ReconDiffAssignRequest;
import com.payflow.admin.dto.recon.ReconDiffCommentRequest;
import com.payflow.admin.dto.recon.ReconDiffCompleteRequest;
import com.payflow.admin.dto.recon.ReconDiffSlaRuleUpsertRequest;
import com.payflow.admin.dto.recon.ReconDiffStartRequest;
import com.payflow.admin.dto.recon.ReconLongTailAcceptLossRequest;
import com.payflow.admin.dto.recon.ReconReportSubscribeRequest;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconHandlerAuditEntity;
import com.payflow.admin.entity.recon.ReconMerchantTaskEntity;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.enums.NotificationTypeEnum;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconHandlerAuditEntityMapper;
import com.payflow.admin.mapper.recon.ReconMerchantTaskEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import com.payflow.admin.service.recon.ReconAggregationService;
import com.payflow.admin.service.recon.ReconDiffWorkflowService;
import com.payflow.admin.service.recon.ReconLongTailService;
import com.payflow.admin.service.recon.ReconReportService;
import com.payflow.admin.service.recon.ReconSlaService;
import com.payflow.admin.service.AdminReconQueryService;
import com.payflow.admin.service.NotificationService;
import com.payflow.common.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 对账管理：统一由 admin-server 提供 HTTP 接口（同库直连）。
 *
 * @author PayFlow Team
 */
@Slf4j
@Tag(name = "对账管理")
@RestController
@RequestMapping("/api/v1/admin/reconcile")
@RequiredArgsConstructor
public class AdminReconController {

    private final ReconTaskEntityMapper reconTaskEntityMapper;
    private final ReconMerchantTaskEntityMapper reconMerchantTaskEntityMapper;
    private final ReconDiffEntityMapper reconDiffEntityMapper;
    private final ReconHandlerAuditEntityMapper reconHandlerAuditEntityMapper;
    private final AdminReconQueryService adminReconQueryService;
    private final NotificationService notificationService;
    private final ReconDiffWorkflowService reconDiffWorkflowService;
    private final ReconSlaService reconSlaService;
    private final ReconAggregationService reconAggregationService;
    private final ReconLongTailService reconLongTailService;
    private final ReconReportService reconReportService;

    private static long capSize(long size) {
        return Math.min(Math.max(size, 1), 500);
    }

    @Operation(summary = "SLA 规则列表")
    @GetMapping("/sla-rules")
    @RequirePermission("recon:manage")
    public ResponseEntity<Map<String, Object>> listSlaRules() {
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", reconSlaService.listRules()));
    }

    @Operation(summary = "SLA 规则保存（按 diffType upsert）")
    @PutMapping("/sla-rules/{diffType}")
    @RequirePermission("recon:manage")
    public ResponseEntity<Map<String, Object>> upsertSlaRule(
            HttpServletRequest request,
            @PathVariable("diffType") String diffType,
            @Valid @RequestBody ReconDiffSlaRuleUpsertRequest body) {
        String operator = String.valueOf(request.getAttribute("username"));
        reconSlaService.upsert(diffType, body, operator);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of("diffType", diffType)));
    }

    /**
     * 按订单/支付维度查询对账结果（全量成功支付 + 差异匹配；onlyAbnormal=true 时仅差异表）。
     */
    @Operation(summary = "查询对账结果")
    @GetMapping("/order-results")
    public ResponseEntity<Map<String, Object>> orderResults(
            HttpServletRequest request,
            @RequestParam LocalDate billDate,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String orderKeyword,
            @RequestParam(defaultValue = "false") boolean onlyAbnormal,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Map<String, Object> data = adminReconQueryService.pageOrderResults(
                billDate, channel, merchantId, orderKeyword, onlyAbnormal, page, capSize(size),
                AdminRequestContext.merchantScope(request));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    /**
     * 对账日汇总：本地成功收款 vs 渠道账单金额、待处理差异笔数。
     */
    @Operation(summary = "对账日汇总")
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> reconSummary(
            HttpServletRequest request,
            @RequestParam LocalDate billDate,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String accountCode) {
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", adminReconQueryService.buildSummary(billDate, channel, accountCode,
                        AdminRequestContext.merchantScope(request))));
    }

    /**
     * 异常（差异）明细分页，供汇总页查看产生差额的订单。
     */
    @Operation(summary = "异常差异明细分页")
    @GetMapping("/anomalies")
    public ResponseEntity<Map<String, Object>> anomalies(
            HttpServletRequest request,
            @RequestParam LocalDate billDate,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String accountCode,
            @RequestParam(required = false) String handleStatus,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Map<String, Object> data = adminReconQueryService.pageAnomalies(
                billDate, channel, accountCode, handleStatus, page, size,
                AdminRequestContext.merchantScope(request));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    @Operation(summary = "查询对账任务列表")
    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> listTasks(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) LocalDate billDate,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        Page<ReconTaskEntity> p = new Page<>(page, size);
        var w = Wrappers.<ReconTaskEntity>lambdaQuery();
        if (billDate != null) {
            w.eq(ReconTaskEntity::getBillDate, billDate);
        }
        if (channel != null && !channel.isBlank()) {
            w.eq(ReconTaskEntity::getChannel, channel);
        }
        if (status != null && !status.isBlank()) {
            w.eq(ReconTaskEntity::getStatus, status);
        }
        w.orderByDesc(ReconTaskEntity::getCreatedAt);
        reconTaskEntityMapper.selectPage(p, w);
        // 对已完成且有差异的任务触发通知（幂等）
        for (ReconTaskEntity task : p.getRecords()) {
            notifyReconDiffIfNeeded(task);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", p.getRecords());
        result.put("total", p.getTotal());
        result.put("page", p.getCurrent());
        result.put("size", p.getSize());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", result));
    }

    @Operation(summary = "查询商户对账任务列表")
    @GetMapping("/merchant-tasks")
    public ResponseEntity<Map<String, Object>> listMerchantTasks(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) LocalDate billDate,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status) {
        Page<ReconMerchantTaskEntity> p = new Page<>(page, size);
        var w = Wrappers.<ReconMerchantTaskEntity>lambdaQuery();
        if (billDate != null) {
            w.eq(ReconMerchantTaskEntity::getBillDate, billDate);
        }
        if (merchantId != null && !merchantId.isBlank()) {
            w.eq(ReconMerchantTaskEntity::getMerchantId, merchantId);
        }
        if (status != null && !status.isBlank()) {
            w.eq(ReconMerchantTaskEntity::getStatus, status);
        }
        w.orderByDesc(ReconMerchantTaskEntity::getCreatedAt);
        reconMerchantTaskEntityMapper.selectPage(p, w);
        Map<String, Object> result = new HashMap<>();
        result.put("list", p.getRecords());
        result.put("total", p.getTotal());
        result.put("page", p.getCurrent());
        result.put("size", p.getSize());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", result));
    }

    @Operation(summary = "下载商户对账文件")
    @GetMapping("/merchant-tasks/{merchantTaskId}/file")
    public ResponseEntity<byte[]> downloadMerchantTaskFile(@PathVariable String merchantTaskId) throws Exception {
        ReconMerchantTaskEntity t = reconMerchantTaskEntityMapper.selectOne(
                Wrappers.<ReconMerchantTaskEntity>lambdaQuery()
                        .eq(ReconMerchantTaskEntity::getMerchantTaskId, merchantTaskId));
        if (t == null) {
            throw new BizException(7545, "商户对账任务不存在: " + merchantTaskId);
        }
        if (t.getStatementObjectKey() == null || t.getStatementObjectKey().isBlank()) {
            throw new BizException(7546, "任务尚无对账单文件: " + merchantTaskId);
        }
        Path p = Path.of(t.getStatementObjectKey());
        byte[] bytes = Files.readAllBytes(p);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"merchant_recon_" + merchantTaskId + ".csv\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @Operation(summary = "查询对账任务详情")
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> taskDetail(@PathVariable String taskId) {
        ReconTaskEntity t = reconTaskEntityMapper.selectOne(
                Wrappers.<ReconTaskEntity>lambdaQuery().eq(ReconTaskEntity::getTaskId, taskId));
        if (t == null) {
            throw new BizException(7540, "对账任务不存在: " + taskId);
        }
        notifyReconDiffIfNeeded(t);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", t));
    }

    /**
     * 对账任务完成且存在差异时，发送站内通知（幂等，同 bizKey 不重复）。
     */
    private void notifyReconDiffIfNeeded(ReconTaskEntity task) {
        if (!"SUCCESS".equals(task.getStatus()) || task.getDiffCount() == null || task.getDiffCount() <= 0) {
            return;
        }
        try {
            notificationService.sendToRole(
                    NotificationTypeEnum.RECON_DIFF,
                    task.getTaskId(),
                    "对账发现差异",
                    "对账任务 " + task.getTaskId() + " 发现 " + task.getDiffCount() + " 笔差异",
                    "/admin/reconcile/tasks/" + task.getTaskId() + "/diffs",
                    null,
                    "recon:manage");
        } catch (Exception e) {
            log.warn("发送对账差异通知失败: taskId={}", task.getTaskId(), e);
        }
    }

    @Operation(summary = "查询对账差异明细")
    @GetMapping("/tasks/{taskId}/diffs")
    public ResponseEntity<Map<String, Object>> listDiffs(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String diffType,
            @RequestParam(required = false) String handleStatus) {
        Page<ReconDiffEntity> p = new Page<>(page, size);
        var w = Wrappers.<ReconDiffEntity>lambdaQuery().eq(ReconDiffEntity::getTaskId, taskId);
        if (diffType != null && !diffType.isBlank()) {
            w.eq(ReconDiffEntity::getDiffType, diffType);
        }
        if (handleStatus != null && !handleStatus.isBlank()) {
            w.eq(ReconDiffEntity::getHandleStatus, handleStatus);
        }
        w.orderByDesc(ReconDiffEntity::getId);
        reconDiffEntityMapper.selectPage(p, w);
        for (ReconDiffEntity diff : p.getRecords()) {
            reconDiffWorkflowService.ensureWorkItemExists(diff);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", p.getRecords());
        result.put("total", p.getTotal());
        result.put("page", p.getCurrent());
        result.put("size", p.getSize());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", result));
    }

    @Operation(summary = "差异工单列表")
    @GetMapping("/diffs/work-items")
    public ResponseEntity<Map<String, Object>> listWorkItems(
            HttpServletRequest request,
            @RequestParam(required = false) LocalDate billDate,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String diffType,
            @RequestParam(required = false) String workflowStatus,
            @RequestParam(required = false) Boolean onlyMine,
            @RequestParam(required = false) Boolean onlyUnassigned,
            @RequestParam(required = false) Boolean onlyOverdue,
            @RequestParam(required = false) String ageBucket,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        Object username = request.getAttribute("username");
        String operator = username != null ? username.toString() : "admin";
        List<String> scope = AdminRequestContext.merchantScope(request);
        Map<String, Object> data = adminReconQueryService.pageWorkItems(
                billDate, channel, diffType, workflowStatus,
                Boolean.TRUE.equals(onlyMine) ? operator : null,
                Boolean.TRUE.equals(onlyUnassigned),
                Boolean.TRUE.equals(onlyOverdue),
                ageBucket,
                page, capSize(size), scope);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    @Operation(summary = "差异归因看板")
    @GetMapping("/aggregation/dashboard")
    public ResponseEntity<Map<String, Object>> aggregationDashboard(
            HttpServletRequest request,
            @RequestParam LocalDate dateFrom,
            @RequestParam LocalDate dateTo,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String diffType) {
        var data = reconAggregationService.buildDashboard(
                dateFrom, dateTo, channel, diffType, AdminRequestContext.merchantScope(request));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    @Operation(summary = "长尾差异汇总")
    @GetMapping("/long-tail/summary")
    public ResponseEntity<Map<String, Object>> longTailSummary(
            HttpServletRequest request,
            @RequestParam(required = false) LocalDate asOf) {
        LocalDate day = asOf != null ? asOf : LocalDate.now();
        var data = reconLongTailService.buildSummary(day, AdminRequestContext.merchantScope(request));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    @Operation(summary = "批量挂账")
    @RequirePermission("recon:manage")
    @PostMapping("/long-tail/accept-loss")
    public ResponseEntity<Map<String, Object>> longTailAcceptLoss(
            HttpServletRequest request,
            @Valid @RequestBody ReconLongTailAcceptLossRequest body) {
        String operator = String.valueOf(request.getAttribute("username"));
        reconLongTailService.batchAcceptLoss(
                body.getDiffIds(), body.getRemark(), operator, request.getRemoteAddr(),
                AdminRequestContext.merchantScope(request));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    @Operation(summary = "我的报告订阅")
    @GetMapping("/subscriptions")
    @RequirePermission("recon:report:subscribe")
    public ResponseEntity<Map<String, Object>> listSubscriptions(HttpServletRequest request) {
        String operator = String.valueOf(request.getAttribute("username"));
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success",
                "data", reconReportService.listBySubscriber(operator)));
    }

    @Operation(summary = "创建/更新报告订阅")
    @PostMapping("/subscriptions")
    @RequirePermission("recon:report:subscribe")
    public ResponseEntity<Map<String, Object>> subscribe(
            HttpServletRequest request,
            @Valid @RequestBody ReconReportSubscribeRequest body) {
        String operator = String.valueOf(request.getAttribute("username"));
        var dto = reconReportService.subscribe(operator, body);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", dto));
    }

    @Operation(summary = "取消报告订阅")
    @DeleteMapping("/subscriptions/{id}")
    @RequirePermission("recon:report:subscribe")
    public ResponseEntity<Map<String, Object>> unsubscribe(
            HttpServletRequest request,
            @PathVariable long id) {
        String operator = String.valueOf(request.getAttribute("username"));
        reconReportService.unsubscribe(operator, id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    @Operation(summary = "报告快照详情")
    @GetMapping("/reports/{snapshotId}")
    @RequirePermission("recon:report:subscribe")
    public ResponseEntity<Map<String, Object>> reportDetail(
            HttpServletRequest request,
            @PathVariable String snapshotId) {
        String operator = String.valueOf(request.getAttribute("username"));
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success",
                "data", reconReportService.getReportSnapshot(snapshotId, operator)));
    }

    @Operation(summary = "差异工单详情")
    @GetMapping("/diffs/{diffId}")
    public ResponseEntity<Map<String, Object>> workItemDetail(HttpServletRequest request, @PathVariable long diffId) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        Map<String, Object> data = adminReconQueryService.getWorkItemDetail(diffId, scope);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    @Operation(summary = "认领未指派工单")
    @RequirePermission("recon:diff:assign")
    @PostMapping("/diffs/{diffId}/claim")
    public ResponseEntity<Map<String, Object>> claim(HttpServletRequest request, @PathVariable long diffId) {
        Object username = request.getAttribute("username");
        String operator = username != null ? username.toString() : "admin";
        reconDiffWorkflowService.claim(diffId, operator, request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    @Operation(summary = "指派/改派工单")
    @RequirePermission("recon:diff:assign")
    @PostMapping("/diffs/{diffId}/assign")
    public ResponseEntity<Map<String, Object>> assign(
            HttpServletRequest request,
            @PathVariable long diffId,
            @Valid @RequestBody ReconDiffAssignRequest body) {
        Object username = request.getAttribute("username");
        String operator = username != null ? username.toString() : "admin";
        reconDiffWorkflowService.assign(diffId, body, operator, request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    @Operation(summary = "开始处理工单")
    @RequirePermission("recon:diff:handle")
    @PostMapping("/diffs/{diffId}/start")
    public ResponseEntity<Map<String, Object>> start(
            HttpServletRequest request,
            @PathVariable long diffId,
            @Valid @RequestBody ReconDiffStartRequest body) {
        Object username = request.getAttribute("username");
        String operator = username != null ? username.toString() : "admin";
        reconDiffWorkflowService.start(diffId, body, operator, request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    @Operation(summary = "提交工单终态")
    @RequirePermission("recon:diff:handle")
    @PostMapping("/diffs/{diffId}/complete")
    public ResponseEntity<Map<String, Object>> complete(
            HttpServletRequest request,
            @PathVariable long diffId,
            @Valid @RequestBody ReconDiffCompleteRequest body) {
        Object username = request.getAttribute("username");
        String operator = username != null ? username.toString() : "admin";
        reconDiffWorkflowService.complete(diffId, body, operator, request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    @Operation(summary = "工单留言")
    @RequirePermission("recon:diff:handle")
    @PostMapping("/diffs/{diffId}/comment")
    public ResponseEntity<Map<String, Object>> comment(
            HttpServletRequest request,
            @PathVariable long diffId,
            @Valid @RequestBody ReconDiffCommentRequest body) {
        Object username = request.getAttribute("username");
        String operator = username != null ? username.toString() : "admin";
        reconDiffWorkflowService.comment(diffId, body, operator, request.getRemoteAddr());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    /**
     * 文件下载：当前默认支持本地存储（file_object_key 为绝对路径）。
     */
    @Operation(summary = "下载对账文件")
    @GetMapping("/tasks/{taskId}/file")
    public ResponseEntity<byte[]> downloadTaskFile(@PathVariable String taskId) throws Exception {
        ReconTaskEntity t = reconTaskEntityMapper.selectOne(
                Wrappers.<ReconTaskEntity>lambdaQuery().eq(ReconTaskEntity::getTaskId, taskId));
        if (t == null) {
            throw new BizException(7540, "对账任务不存在: " + taskId);
        }
        if (t.getFileObjectKey() == null || t.getFileObjectKey().isBlank()) {
            throw new BizException(7541, "任务无对账文件: " + taskId);
        }
        Path p = Path.of(t.getFileObjectKey());
        byte[] bytes = Files.readAllBytes(p);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recon_" + taskId + ".csv\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @Operation(summary = "手动触发对账")
    @RequirePermission("recon:manual_run")
    @PostMapping("/tasks/manual-run")
    public ResponseEntity<Map<String, Object>> manualRun(@Valid @RequestBody ManualReconRequest request) {
        String channel = request.getReconChannel().trim().toLowerCase();
        String taskId = "RECON-" + UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        ReconTaskEntity existing = reconTaskEntityMapper.selectOne(
                Wrappers.<ReconTaskEntity>lambdaQuery()
                        .eq(ReconTaskEntity::getChannel, channel)
                        .eq(ReconTaskEntity::getAccountCode, request.getAccountCode())
                        .eq(ReconTaskEntity::getBillDate, request.getBillDate())
                        .eq(ReconTaskEntity::getBillType, "trade"));
        if (existing != null) {
            taskId = existing.getTaskId();
            existing.setStatus("INIT");
            existing.setTriggeredBy("MANUAL");
            existing.setErrorMsg(null);
            existing.setUpdatedAt(now);
            reconTaskEntityMapper.updateById(existing);
        } else {
            ReconTaskEntity t = new ReconTaskEntity();
            t.setTaskId(taskId);
            t.setChannel(channel);
            t.setAccountCode(request.getAccountCode());
            t.setBillDate(request.getBillDate());
            t.setBillType("trade");
            t.setStatus("INIT");
            t.setDiffCount(0);
            t.setTriggeredBy("MANUAL");
            t.setCreatedAt(now);
            t.setUpdatedAt(now);
            reconTaskEntityMapper.insert(t);
        }
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of("taskId", taskId)));
    }

    @Operation(summary = "处理对账差异")
    @RequirePermission("recon:diff:handle")
    @PostMapping("/diffs/{id}/handle")
    public ResponseEntity<Map<String, Object>> handleDiff(
            HttpServletRequest http,
            @PathVariable long id,
            @Valid @RequestBody HandleReconDiffRequest request) {
        Object username = http.getAttribute("username");
        String operator = username != null ? username.toString() : "admin";
        ReconDiffEntity diff = reconDiffEntityMapper.selectById(id);
        if (diff == null) {
            throw new BizException(7543, "差异记录不存在: " + id);
        }
        String action = request.getAction().trim().toUpperCase();
        if (!"PROCESSED".equals(action) && !"IGNORED".equals(action)) {
            throw new BizException(7544, "action 必须为 PROCESSED 或 IGNORED");
        }
        LocalDateTime now = LocalDateTime.now();
        diff.setHandleStatus(action);
        diff.setHandleRemark(request.getRemark());
        diff.setHandledBy(operator);
        diff.setHandledAt(now);
        reconDiffEntityMapper.updateById(diff);

        ReconHandlerAuditEntity audit = new ReconHandlerAuditEntity();
        audit.setDiffId(id);
        audit.setAction(action);
        audit.setOperator(operator);
        audit.setDetail(request.getRemark());
        audit.setClientIp(http.getRemoteAddr());
        audit.setCreatedAt(now);
        reconHandlerAuditEntityMapper.insert(audit);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }
}
