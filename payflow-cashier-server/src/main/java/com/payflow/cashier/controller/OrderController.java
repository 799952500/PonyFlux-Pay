package com.payflow.cashier.controller;

import com.payflow.cashier.dto.CreateOrderRequest;
import com.payflow.cashier.dto.CreateOrderResponse;
import com.payflow.cashier.dto.OrderDetailResponse;
import com.payflow.common.web.R;
import com.payflow.cashier.context.MerchantContext;
import com.payflow.cashier.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 *
 * @author PayFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "订单管理", description = "订单创建与查询")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * POST /api/v1/orders - 创建订单
     * 需要 JWT Token
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "创建订单", description = "创建支付订单，返回收银台跳转链接")
    public R<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {

        String merchantId = MerchantContext.getMerchantId();
        request.setMerchantId(merchantId);

        log.info("创建订单: merchantId={}, merchantOrderNo={}", merchantId, request.getMerchantOrderNo());
        CreateOrderResponse response = orderService.createOrder(request);
        return R.ok(response);
    }

    /**
     * GET /api/v1/orders/{orderId} - 查询订单详情
     * 需要 JWT Token
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "查询订单详情", description = "根据订单号查询订单详情")
    public R<OrderDetailResponse> getOrderDetail(@PathVariable String orderId) {
        log.debug("查询订单详情: orderId={}", orderId);
        OrderDetailResponse response = orderService.getOrderDetail(orderId);
        return R.ok(response);
    }
}
