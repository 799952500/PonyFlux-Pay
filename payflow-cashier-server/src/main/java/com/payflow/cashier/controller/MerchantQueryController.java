package com.payflow.cashier.controller;

import com.payflow.cashier.dto.OrderDetailResponse;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * 商户查询接口
 * 供商户侧系统查询订单状态
 *
 * 签名验证：由安全工程师的 SignatureInterceptor 统一拦截处理
 * 本 Controller 只负责业务逻辑
 *
 * @author PayFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
@Tag(name = "商户查询", description = "供商户侧系统查询订单状态")
public class MerchantQueryController {

    private final OrderService orderService;

    /**
     * 商户查询订单详情
     * GET /api/v1/merchant/orders/{orderId}?merchantId={merchantId}&timestamp={ts}&sign={sign}
     *
     * 签名验证：SignatureInterceptor（安全工程师实现后接入）
     * 优先查 Redis 缓存，未命中查 DB
     *
     * @param orderId    平台订单号
     * @param merchantId 商户号
     * @param timestamp  时间戳（用于签名验证）
     * @param sign       签名（用于签名验证）
     * @return 订单详情
     */
    @GetMapping("/orders/{orderId}")
    @Operation(
            summary = "商户查询订单详情",
            description = "供商户侧系统查询订单状态，优先读Redis缓存，签名由SignatureInterceptor验证"
    )
    @ResponseStatus(HttpStatus.OK)
    public R<OrderDetailResponse> queryOrder(
            @Parameter(description = "平台订单号", required = true)
            @PathVariable String orderId,

            @Parameter(description = "商户号", required = true)
            @RequestParam String merchantId,

            @Parameter(description = "时间戳（秒）", required = true)
            @RequestParam String timestamp,

            @Parameter(description = "签名", required = true)
            @RequestParam String sign) {

        log.info("[商户查询] 查询订单: orderId={}, merchantId={}", orderId, merchantId);

        OrderDetailResponse resp = orderService.getOrderByMerchant(orderId, merchantId);

        if (resp == null) {
            log.warn("[商户查询] 订单不存在或merchantId不匹配: orderId={}, merchantId={}",
                    orderId, merchantId);
            return R.orderNotFound(orderId);
        }

        return R.ok(resp);
    }
}
