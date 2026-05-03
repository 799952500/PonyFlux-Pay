package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.cashier.Refund;
import com.payflow.admin.mapper.cashier.RefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        long pendingRefunds = refundMapper.selectCount(
                new LambdaQueryWrapper<Refund>().eq(Refund::getStatus, Refund.STATUS_REFUNDING));
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("pendingRefunds", pendingRefunds);
        data.put("announcements", List.of());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
