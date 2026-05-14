package com.payflow.admin.controller;

import com.payflow.admin.entity.MerchantFeeSnapshot;
import com.payflow.admin.service.FeeRateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商户费率查询 Controller
 *
 * @author PayFlow Team
 */
@Tag(name = "商户费率")
@RestController
@RequestMapping("/api/v1/admin/merchant-fee")
@RequiredArgsConstructor
public class MerchantFeeController {

    private final FeeRateService feeRateService;

    @Operation(summary = "商户费率进度")
    @GetMapping("/{merchantId}/progress")
    public ResponseEntity<Map<String, Object>> progress(@PathVariable Long merchantId) {
        MerchantFeeSnapshot snapshot = feeRateService.getMerchantProgress(merchantId);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data",
                snapshot != null ? snapshot : Map.of()));
    }

    @Operation(summary = "商户费率历史")
    @GetMapping("/{merchantId}/history")
    public ResponseEntity<Map<String, Object>> history(@PathVariable Long merchantId) {
        List<MerchantFeeSnapshot> history = feeRateService.getMerchantHistory(merchantId);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", history));
    }
}
