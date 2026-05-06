package com.payflow.recon.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.common.exception.BizException;
import com.payflow.recon.dto.FileUrlResponse;
import com.payflow.recon.dto.HandleDiffRequest;
import com.payflow.recon.dto.RunReconRequest;
import com.payflow.recon.entity.ReconDiff;
import com.payflow.recon.entity.ReconHandlerAudit;
import com.payflow.recon.entity.ReconTask;
import com.payflow.recon.exception.R;
import com.payflow.recon.mapper.ReconDiffMapper;
import com.payflow.recon.mapper.ReconHandlerAuditMapper;
import com.payflow.recon.mapper.ReconTaskMapper;
import com.payflow.recon.service.ReconExecuteService;
import com.payflow.recon.storage.ReconFileStorage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
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

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 对账内部 API（管理端代理访问）。
 *
 * @author PayFlow Team
 */
@RestController
@RequestMapping("/api/v1/internal/recon")
@RequiredArgsConstructor
@Tag(name = "对账内部接口", description = "需 X-Payflow-Internal-Token")
public class ReconInternalController {

    public static final String HEADER_OPERATOR = "X-Payflow-Operator";

    private final ReconTaskMapper reconTaskMapper;
    private final ReconDiffMapper reconDiffMapper;
    private final ReconHandlerAuditMapper reconHandlerAuditMapper;
    private final ReconExecuteService reconExecuteService;
    private final ReconFileStorage reconFileStorage;

    @GetMapping("/tasks")
    @Operation(summary = "分页查询对账任务")
    public R<Page<ReconTask>> pageTasks(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) LocalDate billDate,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        Page<ReconTask> p = new Page<>(page, size);
        LambdaQueryWrapper<ReconTask> w = Wrappers.lambdaQuery();
        if (billDate != null) {
            w.eq(ReconTask::getBillDate, billDate);
        }
        if (channel != null && !channel.isBlank()) {
            w.eq(ReconTask::getChannel, channel);
        }
        if (status != null && !status.isBlank()) {
            w.eq(ReconTask::getStatus, status);
        }
        w.orderByDesc(ReconTask::getCreatedAt);
        reconTaskMapper.selectPage(p, w);
        return R.ok(p);
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "任务详情")
    public R<ReconTask> taskDetail(@PathVariable String taskId) {
        ReconTask t = reconTaskMapper.selectOne(
                Wrappers.<ReconTask>lambdaQuery().eq(ReconTask::getTaskId, taskId));
        if (t == null) {
            throw new BizException(7540, "对账任务不存在: " + taskId);
        }
        return R.ok(t);
    }

    @GetMapping("/tasks/{taskId}/diffs")
    @Operation(summary = "任务差异分页")
    public R<Page<ReconDiff>> pageDiffs(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String diffType,
            @RequestParam(required = false) String handleStatus) {
        ensureTask(taskId);
        Page<ReconDiff> p = new Page<>(page, size);
        LambdaQueryWrapper<ReconDiff> w = Wrappers.<ReconDiff>lambdaQuery().eq(ReconDiff::getTaskId, taskId);
        if (diffType != null && !diffType.isBlank()) {
            w.eq(ReconDiff::getDiffType, diffType);
        }
        if (handleStatus != null && !handleStatus.isBlank()) {
            w.eq(ReconDiff::getHandleStatus, handleStatus);
        }
        w.orderByDesc(ReconDiff::getId);
        reconDiffMapper.selectPage(p, w);
        return R.ok(p);
    }

    @GetMapping("/tasks/{taskId}/file-url")
    @Operation(summary = "获取下载地址（预签名或内部路径）")
    public R<FileUrlResponse> fileUrl(@PathVariable String taskId) {
        ReconTask t = ensureTask(taskId);
        if (t.getFileObjectKey() == null || t.getFileObjectKey().isBlank()) {
            throw new BizException(7541, "任务无对账文件: " + taskId);
        }
        String presign = reconFileStorage.presignGet(t.getFileObjectKey(), Duration.ofMinutes(5));
        String internal = "/api/v1/internal/recon/tasks/" + taskId + "/download";
        return R.ok(FileUrlResponse.builder()
                .presignedUrl(presign)
                .internalDownloadPath(internal)
                .build());
    }

    @GetMapping("/tasks/{taskId}/download")
    @Operation(summary = "直接下载对账原始文件")
    public ResponseEntity<InputStreamResource> download(@PathVariable String taskId) {
        ReconTask t = ensureTask(taskId);
        if (t.getFileObjectKey() == null || t.getFileObjectKey().isBlank()) {
            throw new BizException(7541, "任务无对账文件: " + taskId);
        }
        try {
            InputStream in = reconFileStorage.open(t.getFileObjectKey());
            InputStreamResource body = new InputStreamResource(in);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recon_" + taskId + ".csv\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(body);
        } catch (Exception e) {
            throw new BizException(7542, "读取对账文件失败: " + e.getMessage());
        }
    }

    @PostMapping("/tasks/manual-run")
    @Operation(summary = "手动触发对账")
    public R<Map<String, String>> manualRun(@Valid @RequestBody RunReconRequest request) {
        String taskId = reconExecuteService.execute(
                request.getReconChannel().trim().toLowerCase(),
                request.getAccountCode(),
                request.getBillDate(),
                "MANUAL",
                null);
        return R.ok(Map.of("taskId", taskId));
    }

    @PostMapping("/diffs/{id}/handle")
    @Operation(summary = "处理差异")
    public R<Void> handleDiff(@PathVariable Long id,
                              @Valid @RequestBody HandleDiffRequest request,
                              HttpServletRequest http) {
        ReconDiff diff = reconDiffMapper.selectById(id);
        if (diff == null) {
            throw new BizException(7543, "差异记录不存在: " + id);
        }
        String action = request.getAction().trim().toUpperCase();
        if (!"PROCESSED".equals(action) && !"IGNORED".equals(action)) {
            throw new BizException(7544, "action 必须为 PROCESSED 或 IGNORED");
        }
        String operator = request.getOperator();
        if (operator == null || operator.isBlank()) {
            operator = http.getHeader(HEADER_OPERATOR);
        }
        if (operator == null || operator.isBlank()) {
            operator = "system";
        }
        diff.setHandleStatus(action);
        diff.setHandleRemark(request.getRemark());
        diff.setHandledBy(operator);
        diff.setHandledAt(LocalDateTime.now());
        reconDiffMapper.updateById(diff);

        reconHandlerAuditMapper.insert(ReconHandlerAudit.builder()
                .diffId(id)
                .action(action)
                .operator(operator)
                .detail(request.getRemark())
                .clientIp(http.getRemoteAddr())
                .createdAt(LocalDateTime.now())
                .build());
        return R.ok(null);
    }

    private ReconTask ensureTask(String taskId) {
        ReconTask t = reconTaskMapper.selectOne(
                Wrappers.<ReconTask>lambdaQuery().eq(ReconTask::getTaskId, taskId));
        if (t == null) {
            throw new BizException(7540, "对账任务不存在: " + taskId);
        }
        return t;
    }
}
