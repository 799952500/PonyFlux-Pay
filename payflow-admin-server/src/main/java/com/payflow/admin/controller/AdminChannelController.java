package com.payflow.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/channels")
@RequiredArgsConstructor
public class AdminChannelController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> listChannels() {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                List.of(
                        Map.of("channel", "Alipay", "name", "支付宝", "enabled", true, "priority", 1),
                        Map.of("channel", "WeChatPay", "name", "微信支付", "enabled", true, "priority", 2),
                        Map.of("channel", "UnionPay", "name", "银联支付", "enabled", true, "priority", 3),
                        Map.of("channel", "CreditCard", "name", "信用卡", "enabled", false, "priority", 4)
                )
        ));
    }

    @PutMapping("/{channel}/toggle")
    public ResponseEntity<Map<String, Object>> toggleChannel(@PathVariable String channel) {
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of("channel", channel, "enabled", true)
        ));
    }
}
