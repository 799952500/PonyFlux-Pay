package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.entity.Refund;
import com.payflow.admin.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 退款管理 Controller
 */
@RestController
@RequestMapping("/api/v1/admin/refunds")
@RequiredArgsConstructor
public class AdminRefundController {

    private final RefundService refundService;

    /**
     * 分页查询退款记录列表
     *
     * @param merchantId 商户ID（可选）
     * @param status     退款状态（可选）
     * @param startTime  开始时间（可选）
     * @param endTime    结束时间（可选）
     * @param page       页码（默认1）
     * @param size       每页条数（默认20）
     * @return 分页后的退款列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listRefunds(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        IPage<Refund> refundPage = refundService.page(page, size, merchantId, status, startTime, endTime);
        Map<String, Object> data = new HashMap<>();
        data.put("total", refundPage.getTotal());
        data.put("page", page);
        data.put("size", size);
        data.put("list", refundPage.getRecords());
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    /**
     * 查询退款详情
     *
     * @param id 退款记录ID
     * @return 退款详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getRefund(@PathVariable Long id) {
        Refund refund = refundService.getById(id);
        if (refund == null) {
            return ResponseEntity.ok(Map.of("code", 404, "message", "退款记录不存在", "data", (Object) null));
        }
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", refund));
    }

    /**
     * 审批通过退款申请
     *
     * @param id 退款记录ID
     * @return 操作结果
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id) {
        try {
            refundService.approve(id);
            return ResponseEntity.ok(Map.of("code", 0, "message", "审批通过"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }

    /**
     * 审批拒绝退款申请
     *
     * @param id 退款记录ID
     * @param body 包含拒绝原因
     * @return 操作结果
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        try {
            refundService.reject(id, reason);
            return ResponseEntity.ok(Map.of("code", 0, "message", "已拒绝"));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("code", 400, "message", e.getMessage()));
        }
    }
}
