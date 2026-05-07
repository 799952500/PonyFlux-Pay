package com.payflow.admin.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运营洞察：支付漏斗等占位指标（后续接入真实聚合 SQL）。
 */
@RestController
@RequestMapping("/api/v1/admin/insights")
public class AdminInsightsController {

    @GetMapping("/funnel")
    public Map<String, Object> funnel() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("created", 0L);
        m.put("paying", 0L);
        m.put("paid", 0L);
        m.put("note", "占位数据：请接入 cashier_orders / cashier_payments 聚合统计");
        return m;
    }
}
