package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.ChurnAlert;
import com.payflow.admin.entity.cashier.Refund;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.mapper.ChurnAlertMapper;
import com.payflow.admin.mapper.cashier.RefundMapper;
import com.payflow.admin.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知中心：列表/未读计数/标记已读/摘要。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final RefundMapper refundMapper;
    private final ChurnAlertMapper churnAlertMapper;
    private final NotificationService notificationService;

    /**
     * 通知分页列表。
     */
    @GetMapping("")
    public ResponseEntity<Map<String, Object>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "all") String read,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "1") @Min(1) Integer page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer size) {
        Long userId = (Long) request.getAttribute("userId");
        List<String> merchantScope = AdminRequestContext.merchantScope(request);
        Map<String, Object> data = notificationService.listByUser(userId, merchantScope, read, type, page, size);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    /**
     * 未读总数（顶栏 badge）。
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> unreadCount(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<String> merchantScope = AdminRequestContext.merchantScope(request);
        long count = notificationService.countUnread(userId, merchantScope);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success",
                "data", Map.of("count", count)));
    }

    /**
     * 单条标记已读。
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markRead(
            @PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        boolean ok = notificationService.markRead(id, userId);
        if (!ok) {
            return ResponseEntity.ok(Map.of("code", 8001, "message", "通知不存在或不属于当前用户", "data", Map.of()));
        }
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    /**
     * 全部标记已读。
     */
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<String> merchantScope = AdminRequestContext.merchantScope(request);
        int affected = notificationService.markAllRead(userId, merchantScope);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success",
                "data", Map.of("affected", affected)));
    }

    /**
     * 批量标记已读。
     */
    @PostMapping("/read-batch")
    public ResponseEntity<Map<String, Object>> markBatchRead(
            @RequestBody Map<String, List<Long>> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Long> ids = body.getOrDefault("ids", List.of());
        int affected = notificationService.markBatchRead(ids, userId);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success",
                "data", Map.of("affected", affected)));
    }

    /**
     * 摘要（兼容旧接口 + 扩展 unreadCount）。
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(HttpServletRequest request) {
        long pendingRefunds = refundMapper.selectCount(
                new LambdaQueryWrapper<Refund>().eq(Refund::getStatus, Refund.STATUS_REFUNDING));

        long pendingChurnAlerts = churnAlertMapper.selectCount(
                new LambdaQueryWrapper<ChurnAlert>().eq(ChurnAlert::getStatus, "pending"));
        long overdueChurnAlerts = churnAlertMapper.selectCount(
                new LambdaQueryWrapper<ChurnAlert>()
                        .eq(ChurnAlert::getStatus, "pending")
                        .lt(ChurnAlert::getCreateTime, LocalDateTime.now().minusHours(48)));

        Long userId = (Long) request.getAttribute("userId");
        List<String> merchantScope = AdminRequestContext.merchantScope(request);
        long unreadCount = notificationService.countUnread(userId, merchantScope);

        Map<String, Object> data = new LinkedHashMap<>(8);
        data.put("pendingRefunds", pendingRefunds);
        data.put("pendingChurnAlerts", pendingChurnAlerts);
        data.put("overdueChurnAlerts", overdueChurnAlerts);
        data.put("unreadCount", unreadCount);
        data.put("announcements", List.of());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
