package com.payflow.admin.controller;

import com.payflow.admin.entity.cashier.Refund;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 前后端共用的枚举字典（减少魔法字符串）。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/dicts")
public class AdminDictController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> all() {
        Map<String, Object> refundStatus = new LinkedHashMap<>();
        refundStatus.put("REFUNDING", "退款处理中");
        refundStatus.put("REFUNDED", "退款成功");
        refundStatus.put("FAILED", "退款失败");
        refundStatus.put("CLOSED", "退款关闭");

        Map<String, Object> orderStatus = new LinkedHashMap<>();
        orderStatus.put("CREATED", "待支付");
        orderStatus.put("PAYING", "支付中");
        orderStatus.put("PAID", "已支付");
        orderStatus.put("SUCCESS", "成功");
        orderStatus.put("EXPIRED", "已过期");
        orderStatus.put("FAILED", "失败");
        orderStatus.put("CLOSED", "已关闭");
        orderStatus.put("REFUNDED", "已退款");

        Map<String, Object> payChannels = new LinkedHashMap<>();
        payChannels.put("ALIPAY", "支付宝");
        payChannels.put("WECHAT_PAY", "微信支付");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("refundStatus", refundStatus);
        data.put("orderStatus", orderStatus);
        data.put("payChannels", payChannels);
        data.put("refundUiMapping", List.of(
                Map.of("db", Refund.STATUS_REFUNDING, "ui", "PENDING", "label", "待处理"),
                Map.of("db", Refund.STATUS_REFUNDED, "ui", "COMPLETED", "label", "已完成"),
                Map.of("db", Refund.STATUS_FAILED, "ui", "REJECTED", "label", "已拒绝")
        ));

        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
