package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.ChurnAlert;
import com.payflow.admin.entity.cashier.Refund;
import com.payflow.admin.mapper.ChurnAlertMapper;
import com.payflow.admin.mapper.cashier.RefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知摘要（待办笔数等）。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final RefundMapper refundMapper;
    private final ChurnAlertMapper churnAlertMapper;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        long pendingRefunds = refundMapper.selectCount(
                new LambdaQueryWrapper<Refund>().eq(Refund::getStatus, Refund.STATUS_REFUNDING));

        // 流失预警统计
        long pendingChurnAlerts = churnAlertMapper.selectCount(
                new LambdaQueryWrapper<ChurnAlert>().eq(ChurnAlert::getStatus, "pending"));
        long overdueChurnAlerts = churnAlertMapper.selectCount(
                new LambdaQueryWrapper<ChurnAlert>()
                        .eq(ChurnAlert::getStatus, "pending")
                        .lt(ChurnAlert::getCreateTime, LocalDateTime.now().minusHours(48)));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingRefunds", pendingRefunds);
        data.put("pendingChurnAlerts", pendingChurnAlerts);
        data.put("overdueChurnAlerts", overdueChurnAlerts);
        data.put("announcements", List.of());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
