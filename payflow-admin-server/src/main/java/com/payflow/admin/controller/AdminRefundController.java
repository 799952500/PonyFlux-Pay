package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.service.AdminRefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理端退款列表与审批接口（读写 cashier 库）。
 *
 * @author Lucas
 */
@Tag(name = "退款管理")
@RestController
@RequestMapping("/api/v1/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final AdminRefundService adminRefundService;

    /**
     * 分页查询退款列表。
     */
    @Operation(summary = "分页查询退款列表")
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        IPage<Map<String, Object>> p = adminRefundService.page(page, pageSize, merchantId, status, keyword, startDate, endDate,
                AdminRequestContext.merchantScope(request));
        Map<String, Object> data = new HashMap<>();
        data.put("list", p.getRecords());
        data.put("total", p.getTotal());
        data.put("page", page);
        data.put("pageSize", pageSize);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    /**
     * 审批通过。
     */
    @Operation(summary = "审批通过退款")
    @PostMapping("/{refundId}/approve")
    public ResponseEntity<Map<String, Object>> approve(HttpServletRequest request, @PathVariable String refundId) {
        try {
            adminRefundService.approve(refundId, AdminRequestContext.merchantScope(request));
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("code", 404, "message", e.getMessage(), "data", Map.of()));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage(), "data", Map.of()));
        }
    }

    /**
     * 审批拒绝。
     */
    @Operation(summary = "审批拒绝退款")
    @PostMapping("/{refundId}/reject")
    public ResponseEntity<Map<String, Object>> reject(HttpServletRequest request, @PathVariable String refundId) {
        try {
            adminRefundService.reject(refundId, AdminRequestContext.merchantScope(request));
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("code", 404, "message", e.getMessage(), "data", Map.of()));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage(), "data", Map.of()));
        }
    }
}
