package com.payflow.cashier.controller;

import com.payflow.cashier.dto.OrderDetailResponse;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.middleware.MerchantSignatureInterceptor;
import com.payflow.cashier.service.OrderService;
import com.payflow.cashier.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 商户查询接口：供商户服务端查询订单与支付状态。
 * <p>
 * 鉴权由 {@link MerchantSignatureInterceptor} 完成，请求须携带：
 * {@code X-Merchant-Id}、{@code X-Timestamp}（Unix 秒）、{@code X-Sign}（HMAC-SHA256 十六进制小写）。
 * 签名原文：HTTP_METHOD + "\n" + path + "\n" + 按字典序排序的 queryString + "\n" + timestamp
 * </p>
  * @author Lucas
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
@Tag(name = "商户查询", description = "供商户侧系统查询订单与支付状态")
public class MerchantQueryController {

    private final OrderService orderService;
    private final PaymentService paymentService;

    /**
     * 商户查询订单详情。
     * <p>
     * 路径：GET /api/v1/merchant/orders/{orderId}，query 参数须参与签名（如 merchantId 用于业务校验）。
     * </p>
     */
    @GetMapping("/orders/{orderId}")
    @Operation(summary = "商户查询订单详情", description = "优先读 Redis 缓存；须通过商户 HMAC 头验签")
    @ResponseStatus(HttpStatus.OK)
    public R<OrderDetailResponse> queryOrder(
            @Parameter(description = "平台订单号", required = true)
            @PathVariable String orderId,

            @Parameter(description = "商户号（须与 X-Merchant-Id 一致）", required = true)
            @RequestParam String merchantId,

            @Parameter(description = "时间戳（秒，须与 X-Timestamp 一致）", required = true)
            @RequestParam String timestamp,

            @Parameter(description = "签名（与 X-Sign 一致，可选用于文档兼容）", required = false)
            @RequestParam(required = false) String sign) {

        log.info("[商户查询] 查询订单: orderId={}, merchantId={}", orderId, merchantId);

        OrderDetailResponse resp = orderService.getOrderByMerchant(orderId, merchantId);

        if (resp == null) {
            log.warn("[商户查询] 订单不存在或merchantId不匹配: orderId={}, merchantId={}",
                    orderId, merchantId);
            return R.orderNotFound(orderId);
        }

        return R.ok(resp);
    }

    /**
     * 商户查询支付单状态。
     */
    @GetMapping("/payments/{paymentId}/status")
    @Operation(summary = "商户查询支付状态", description = "根据 paymentId 返回支付状态字符串")
    @ResponseStatus(HttpStatus.OK)
    public R<Map<String, String>> queryPaymentStatus(
            @PathVariable String paymentId,
            @RequestParam String merchantId,
            @RequestParam String timestamp,
            @RequestParam(required = false) String sign) {

        String status = paymentService.getPaymentStatusForMerchant(merchantId, paymentId);
        return R.ok(Map.of("paymentId", paymentId, "status", status));
    }
}
