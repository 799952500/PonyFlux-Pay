package com.payflow.admin.controller;

import com.payflow.admin.dto.FunnelResult;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.service.FunnelService;
import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * 运营洞察：支付漏斗等指标。
 */
@RestController
@RequestMapping("/api/v1/admin/insights")
@RequiredArgsConstructor
public class AdminInsightsController {

    private final FunnelService funnelService;

    @GetMapping("/funnel")
    public ResponseEntity<Map<String, Object>> funnel(
            HttpServletRequest request,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String channel) {

        if (dateFrom == null) {
            dateFrom = LocalDate.now().minusDays(7);
        }
        if (dateTo == null) {
            dateTo = LocalDate.now();
        }
        if (dateFrom.isAfter(dateTo)) {
            throw new BizException(4001, "dateFrom 不能晚于 dateTo");
        }
        if (ChronoUnit.DAYS.between(dateFrom, dateTo) > 366) {
            throw new BizException(4002, "日期跨度不能超过 366 天");
        }

        List<String> merchantScopeIds = AdminRequestContext.merchantScope(request);
        if (merchantId != null) {
            AdminRequestContext.assertMerchantAllowed(merchantId, merchantScopeIds);
        }

        FunnelResult result = funnelService.queryFunnel(dateFrom, dateTo, merchantId, channel, merchantScopeIds);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", result
        ));
    }
}
