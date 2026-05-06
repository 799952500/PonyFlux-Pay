package com.payflow.admin.controller;

import com.payflow.admin.client.AdminReconClient;
import com.payflow.admin.dto.HandleReconDiffRequest;
import com.payflow.admin.dto.ManualReconRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 对账管理：反向代理至 payflow-recon-server。
 *
 * @author PayFlow Team
 */
@RestController
@RequestMapping("/api/v1/admin/reconcile")
@RequiredArgsConstructor
public class AdminReconController {

    private final AdminReconClient adminReconClient;

    @GetMapping("/tasks")
    public ResponseEntity<Map<String, Object>> listTasks(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate billDate,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String status) {
        Map<String, Object> p = adminReconClient.pageTasks(page, size, billDate, channel, status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", p.get("records"));
        result.put("total", p.get("total"));
        result.put("page", p.get("current"));
        result.put("size", p.get("size"));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", result));
    }

    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> taskDetail(@PathVariable String taskId) {
        Map<String, Object> row = adminReconClient.getTask(taskId);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", row));
    }

    @GetMapping("/tasks/{taskId}/diffs")
    public ResponseEntity<Map<String, Object>> listDiffs(
            @PathVariable String taskId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String diffType,
            @RequestParam(required = false) String handleStatus) {
        Map<String, Object> p = adminReconClient.pageDiffs(taskId, page, size, diffType, handleStatus);
        Map<String, Object> result = new HashMap<>();
        result.put("list", p.get("records"));
        result.put("total", p.get("total"));
        result.put("page", p.get("current"));
        result.put("size", p.get("size"));
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", result));
    }

    /**
     * 预签名 URL 存在时 302；否则由管理端拉取对账服务文件字节并回传（本地存储场景）。
     */
    @GetMapping("/tasks/{taskId}/file")
    public ResponseEntity<?> downloadTaskFile(@PathVariable String taskId) {
        Map<String, Object> meta = adminReconClient.getFileUrl(taskId);
        Object presigned = meta.get("presignedUrl");
        if (presigned != null && !presigned.toString().isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(presigned.toString()))
                    .build();
        }
        byte[] bytes = adminReconClient.downloadTaskFile(taskId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"recon_" + taskId + ".csv\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }

    @PostMapping("/tasks/manual-run")
    public ResponseEntity<Map<String, Object>> manualRun(@Valid @RequestBody ManualReconRequest request) {
        String taskId = adminReconClient.manualRun(
                request.getReconChannel().trim().toLowerCase(),
                request.getAccountCode(),
                request.getBillDate());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of("taskId", taskId)));
    }

    @PostMapping("/diffs/{id}/handle")
    public ResponseEntity<Map<String, Object>> handleDiff(
            HttpServletRequest http,
            @PathVariable long id,
            @Valid @RequestBody HandleReconDiffRequest request) {
        Object username = http.getAttribute("username");
        String operator = username != null ? username.toString() : "admin";
        adminReconClient.handleDiff(id, request.getAction().trim(), request.getRemark(), operator);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }
}
