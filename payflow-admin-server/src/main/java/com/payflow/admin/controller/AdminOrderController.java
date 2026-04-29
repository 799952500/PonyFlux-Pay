package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.entity.cashier.Payment;
import com.payflow.admin.service.OrderService;
import com.payflow.admin.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单管理控制器（查询 cashier 库）
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * 分页查询订单列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listOrders(
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        IPage<Order> orderPage = orderService.page(page, size, merchantId, status, startTime, endTime);
        
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
     * 查询订单详情
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Map<String, Object>> getOrder(@PathVariable String orderId) {
        Order order = orderService.getByOrderId(orderId);
        if (order == null) {
            return ResponseEntity.ok(Map.of(
                    "code", 404, "message", "订单不存在", "data", null
            ));
        }
        
        // 查询关联的支付记录
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
    @GetMapping("/merchant/{merchantId}")
    public ResponseEntity<Map<String, Object>> listByMerchant(@PathVariable String merchantId) {
        List<Order> orders = orderService.findByMerchantId(merchantId);
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data", orders
        ));
    }

    /**
     * 按状态统计订单数量
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<Map<String, Object>> statusCount = orderService.countByStatus();
        long total = orderService.count();
        
        Map<String, Object> data = new HashMap<>();
        data.put("total", total);
        data.put("statusCount", statusCount);
        
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data", data
        ));
    }

    /**
     * 关闭订单
     */
    @PostMapping("/{orderId}/close")
    public ResponseEntity<Map<String, Object>> closeOrder(@PathVariable String orderId) {
        Order order = orderService.getByOrderId(orderId);
        if (order == null) {
            return ResponseEntity.ok(Map.of(
                    "code", 404, "message", "订单不存在", "data", null
            ));
        }
        
        // 只有 CREATED 或 PENDING 状态的订单可以关闭
        if (!Order.STATUS_CREATED.equals(order.getStatus()) && 
            !Order.STATUS_PENDING.equals(order.getStatus())) {
            return ResponseEntity.ok(Map.of(
                    "code", 400, "message", "当前状态不允许关闭", "data", null
            ));
        }
        
        // TODO: 实际关闭订单逻辑（调用 cashier-server API 或直接更新数据库）
        
        return ResponseEntity.ok(Map.of(
                "code", 0, "message", "success", "data",
                Map.of("orderId", orderId, "status", "CLOSED")
        ));
    }
}
