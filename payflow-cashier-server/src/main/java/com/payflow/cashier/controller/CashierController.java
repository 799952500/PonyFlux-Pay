package com.payflow.cashier.controller;

import com.payflow.cashier.dto.CashierResponse;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 收银台控制器
 *
 * 注意：此接口由前端收银台页面调用，无需 JWT 认证
 * 签名校验由前端自行处理（通过 sig 参数）
 *
 * @author PayFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/cashier")
@Tag(name = "收银台", description = "收银台页面数据接口")
public class CashierController {

    private final OrderService orderService;

    public CashierController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * GET /api/v1/cashier/{orderId}?sig={签名}
     * 无需 JWT Token
     */
    @GetMapping("/{orderId}")
    @Operation(summary = "获取收银台信息", description = "获取收银台订单信息和可用支付方式")
    public R<CashierResponse> getCashierInfo(
            @Parameter(description = "平台订单号") @PathVariable String orderId,
            @Parameter(description = "签名（前端已校验）") @RequestParam(required = false) String sig) {

        log.info("收银台访问: orderId={}, sig={}", orderId, sig != null ? "***" : "null");
        CashierResponse response = orderService.getCashierInfo(orderId);
        return R.ok(response);
    }
}
