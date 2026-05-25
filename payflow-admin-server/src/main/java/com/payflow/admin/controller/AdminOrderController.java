package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.entity.cashier.Payment;
import com.payflow.admin.kit.AdminApiResponseKit;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.dto.AdminOrderRefundRequest;
import com.payflow.admin.service.AdminOrderOpsService;
import com.payflow.admin.service.OrderService;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.service.PaymentService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单管理控制器（查询 cashier 库）
 *
 * @author Lucas
 */
@Tag(name = "订单管理")
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final AdminOrderOpsService adminOrderOpsService;

    /**
     * 分页查询订单列表
     */
    @Operation(summary = "查询订单列表")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listOrders(
            HttpServletRequest request,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<String> scope = AdminRequestContext.merchantScope(request);
        IPage<Order> orderPage = orderService.page(page, size, merchantId, status, startTime, endTime, scope);

        Map<String, Object> result = new HashMap<>();
        result.put("total", orderPage.getTotal());
        result.put("page", page);
        result.put("size", size);
        result.put("list", orderPage.getRecords());

        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data", result
        ));
    }

    /**
     * 导出订单 CSV（UTF-8 BOM，最多 5000 行）
     */
    @Operation(summary = "导出订单CSV")
    @RequirePermission("order:export")
    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportOrders(
            HttpServletRequest request,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "3000") int maxRows) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        List<Order> rows = orderService.listForExport(maxRows, merchantId, status, startTime, endTime, scope);
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');
        sb.append("orderId,merchantId,merchantOrderNo,status,amount,currency,createdAt\n");
        for (Order o : rows) {
            sb.append(csvEscape(o.getOrderId())).append(',')
                    .append(csvEscape(o.getMerchantId())).append(',')
                    .append(csvEscape(o.getMerchantOrderNo())).append(',')
                    .append(csvEscape(o.getStatus())).append(',')
                    .append(o.getAmount() != null ? o.getAmount() : "").append(',')
                    .append(csvEscape(o.getCurrency())).append(',')
                    .append(o.getCreatedAt() != null ? o.getCreatedAt().toString() : "")
                    .append('\n');
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders-export.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(sb.toString());
    }

    private static String csvEscape(String s) {
        if (s == null) {
            return "";
        }
        // 防止 CSV 公式注入：以 = + - @ 开头的值添加单引号前缀
        if (!s.isEmpty() && (s.charAt(0) == '=' || s.charAt(0) == '+' || s.charAt(0) == '-' || s.charAt(0) == '@')) {
            s = "'" + s;
        }
        String v = s.replace("\"", "\"\"");
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v + "\"";
        }
        return v;
    }

    /**
     * 查询订单详情
     */
    @Operation(summary = "获取订单详情")
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(HttpServletRequest request, @PathVariable String orderId) {
        Order order = orderService.getByOrderId(orderId);
        if (order == null) {
            return ResponseEntity.ok(Map.of(
                    "code", 404, "message", "订单不存在", "data", null
            ));
        }
        List<String> scope = AdminRequestContext.merchantScope(request);
        if (orderNotVisible(order, scope)) {
            return ResponseEntity.ok(Map.of(
                    "code", 404, "message", "订单不存在", "data", null
            ));
        }

        List<Payment> payments = paymentService.findByOrderId(orderId);

        Map<String, Object> data = new HashMap<>();
        data.put("order", order);
        data.put("payments", payments);

        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data", data
        ));
    }

    /**
     * 按商户查询订单
     */
    @Operation(summary = "按商户查询订单")
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<Map<String, Object>> listByMerchant(HttpServletRequest request,
                                                              @PathVariable String merchantId) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        if (scope != null) {
            if (scope.isEmpty() || !scope.contains(merchantId)) {
                return ResponseEntity.ok(Map.of(
                        "code", 0, "message", "success", "data", List.of()
                ));
            }
        }
        List<Order> orders = orderService.findByMerchantId(merchantId);
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data", orders
        ));
    }

    /**
     * 按状态统计订单数量
     */
    @Operation(summary = "订单状态统计")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            HttpServletRequest request,
            @RequestParam(required = false) String merchantId) {
        List<String> scope = AdminRequestContext.merchantScope(request);
        List<Map<String, Object>> statusCount = orderService.countByStatus(merchantId, scope);
        long total = orderService.count(merchantId, scope);

        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("statusCount", statusCount);

        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data", data
        ));
    }

    /**
     * 发起退款申请（仅创建待审批记录，须在退款管理审批通过后才会调渠道）
     */
    @Operation(summary = "发起退款申请")
    @RequirePermission("refund:create")
    @PostMapping("/{orderId}/refund-requests")
    public ResponseEntity<Map<String, Object>> createRefundRequest(
            HttpServletRequest request,
            @PathVariable String orderId,
            @Valid @RequestBody AdminOrderRefundRequest body) {
        try {
            List<String> scope = AdminRequestContext.merchantScope(request);
            Map<String, Object> data = adminOrderOpsService.createRefundRequest(orderId, body, scope);
            return ResponseEntity.ok(AdminApiResponseKit.success(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(AdminApiResponseKit.error(404, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(AdminApiResponseKit.error(500, e.getMessage()));
        }
    }

    /**
     * 向支付机构查单；sync=true 时尝试将渠道成功结果同步到本地
     */
    @Operation(summary = "查询支付机构订单状态")
    @RequirePermission("order:payment:query")
    @PostMapping("/{orderId}/payments/{paymentId}/query-channel")
    public ResponseEntity<Map<String, Object>> queryPaymentChannel(
            HttpServletRequest request,
            @PathVariable String orderId,
            @PathVariable String paymentId,
            @RequestParam(defaultValue = "false") boolean sync) {
        try {
            List<String> scope = AdminRequestContext.merchantScope(request);
            Map<String, Object> data = adminOrderOpsService.queryPaymentChannel(
                    orderId, paymentId, sync, scope);
            return ResponseEntity.ok(AdminApiResponseKit.success(data));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(AdminApiResponseKit.error(404, e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(AdminApiResponseKit.error(500, e.getMessage()));
        }
    }

    /**
     * 关闭订单
     */
    @Operation(summary = "关闭订单")
    @RequirePermission("order:close")
    @PostMapping("/{orderId}/close")
    public ResponseEntity<Map<String, Object>> closeOrder(HttpServletRequest request,
                                                           @PathVariable String orderId) {
        Order order = orderService.getByOrderId(orderId);
        if (order == null) {
            return ResponseEntity.ok(Map.of(
                    "code", 404, "message", "订单不存在", "data", null
            ));
        }
        List<String> scope = AdminRequestContext.merchantScope(request);
        if (orderNotVisible(order, scope)) {
            return ResponseEntity.ok(Map.of(
                    "code", 404, "message", "订单不存在", "data", null
            ));
        }

        if (!Order.STATUS_CREATED.equals(order.getStatus())
                && !Order.STATUS_PENDING.equals(order.getStatus())) {
            return ResponseEntity.ok(Map.of(
                    "code", 400, "message", "当前状态不允许关闭", "data", null
            ));
        }

        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of("orderId", orderId, "status", "CLOSED")
        ));
    }

    /**
     * scope==null：超管或未启用数据权限；可见。<br>
     * scope 空列表：不可见任何商户数据。<br>
     * 非空列表：仅可见列表内商户。
     */
    private static boolean orderNotVisible(Order order, List<String> scope) {
        if (scope == null) {
            return false;
        }
        if (scope.isEmpty()) {
            return true;
        }
        return order.getMerchantId() == null || !scope.contains(order.getMerchantId());
    }
}
