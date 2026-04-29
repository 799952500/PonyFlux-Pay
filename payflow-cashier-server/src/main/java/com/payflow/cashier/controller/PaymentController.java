package com.payflow.cashier.controller;

import com.payflow.cashier.dto.CreatePaymentRequest;
import com.payflow.cashier.dto.CreatePaymentResponse;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.middleware.MerchantSignatureInterceptor;
import com.payflow.cashier.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 支付控制器
 *
 * @author PayFlow Team
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "支付管理", description = "发起支付与查询支付状态")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * POST /api/v1/payments - 发起支付（须商户 HMAC 请求头，与 {@code /api/v1/merchant/**} 相同规则）。
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "发起支付", description = "商户签名验证通过后发起支付，返回调起参数或二维码")
    public R<CreatePaymentResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request,
            HttpServletRequest httpRequest) {
        // 从拦截器注入的请求属性中获取已验证的商户 ID
        String merchantId = (String) httpRequest.getAttribute(MerchantSignatureInterceptor.ATTR_MERCHANT_ID);
        log.info("发起支付: merchantId={}, orderId={}, payChannel={}, payMethod={}",
                merchantId, request.getOrderId(), request.getPayChannel(), request.getPayMethod());
        CreatePaymentResponse response = paymentService.createPayment(merchantId, request);
        return R.ok(response);
    }

    /**
     * GET /api/v1/payments/status/{paymentId} - 轮询支付状态（公开，无需认证）
     */

    @GetMapping("/status/{paymentId}")
    @Operation(summary = "查询支付状态", description = "轮询支付状态")
    public R<Map<String, String>> getPaymentStatus(@PathVariable String paymentId) {
        log.debug("查询支付状态: paymentId={}", paymentId);
        String status = paymentService.getPaymentStatus(paymentId);
        return R.ok(Map.of("paymentId", paymentId, "status", status));
    }
}
