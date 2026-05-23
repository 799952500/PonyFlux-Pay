package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.service.MerchantNotifyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商户回调记录查询（平台 → 商户）。
 */
@Tag(name = "商户回调记录")
@RestController
@RequestMapping("/api/v1/admin/merchant-notifies")
@RequiredArgsConstructor
public class AdminMerchantNotifyController {

    private final MerchantNotifyQueryService merchantNotifyQueryService;

    @Operation(summary = "分页查询商户回调记录")
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            HttpServletRequest request,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String merchantOrderNo,
            @RequestParam(required = false) String notifyType,
            @RequestParam(required = false) String summaryStatus,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<String> scope = AdminRequestContext.merchantScope(request);
        IPage<Map<String, Object>> resultPage = merchantNotifyQueryService.page(
                page, size, merchantId, orderId, merchantOrderNo, notifyType, summaryStatus,
                startTime, endTime, scope);

        Map<String, Object> data = new HashMap<>();
        data.put("total", resultPage.getTotal());
        data.put("page", page);
        data.put("size", size);
        data.put("list", resultPage.getRecords());

        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    @Operation(summary = "查询商户回调详情")
    @GetMapping("/{notifyId}")
    public ResponseEntity<Map<String, Object>> detail(
            HttpServletRequest request,
            @PathVariable String notifyId) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        Map<String, Object> data = merchantNotifyQueryService.getDetail(notifyId, scope);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    @Operation(summary = "按订单查询商户回调汇总")
    @GetMapping("/by-order/{orderId}")
    public ResponseEntity<Map<String, Object>> byOrder(
            HttpServletRequest request,
            @PathVariable String orderId,
            @RequestParam(required = false) String notifyType) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        Map<String, Object> data = merchantNotifyQueryService.getByOrder(orderId, notifyType, scope);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
