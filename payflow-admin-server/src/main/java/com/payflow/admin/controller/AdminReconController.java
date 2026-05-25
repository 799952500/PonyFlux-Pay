package com.payflow.admin.controller;

import com.payflow.admin.dto.HandleReconDiffRequest;
import com.payflow.admin.dto.ManualReconRequest;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconHandlerAuditEntity;
import com.payflow.admin.entity.recon.ReconMerchantTaskEntity;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconHandlerAuditEntityMapper;
import com.payflow.admin.mapper.recon.ReconMerchantTaskEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import com.payflow.admin.service.AdminReconQueryService;
import com.payflow.common.exception.BizException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 对账管理：统一由 admin-server 提供 HTTP 接口（同库直连）。
 *
 * @author PayFlow Team
 */
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
                billDate, channel, merchantId, orderKeyword, onlyAbnormal, page, size,
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
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", t));
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
        Map<String, Object> result = new HashMap<>();
        result.put("list", p.getRecords());
        result.put("total", p.getTotal());
        result.put("page", p.getCurrent());
        result.put("size", p.getSize());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", result));
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
