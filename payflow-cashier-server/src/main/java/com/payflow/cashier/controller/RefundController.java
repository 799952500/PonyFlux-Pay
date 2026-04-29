package com.payflow.cashier.controller;

import com.payflow.cashier.dto.RefundRequest;
import com.payflow.cashier.dto.RefundResponse;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.middleware.MerchantSignatureInterceptor;
import com.payflow.cashier.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 退款控制器（商户签名保护）。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/refunds")
@Tag(name = "退款管理", description = "支付退款申请与查询")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @Operation(summary = "申请退款", description = "对已支付订单申请退款（需商户 HMAC 头）")
    public R<RefundResponse> refund(
            @Valid @RequestBody RefundRequest request,
            HttpServletRequest httpRequest) {
        String merchantId = (String) httpRequest.getAttribute(MerchantSignatureInterceptor.ATTR_MERCHANT_ID);
        log.info("收到退款申请: merchantId={}, paymentId={}, amount={}",
                merchantId, request.getPaymentId(), request.getRefundAmount());
        RefundResponse response = refundService.refund(merchantId, request);
        return R.ok(response);
    }

    @GetMapping("/{refundId}")
    @Operation(summary = "查询退款", description = "按 refundId 查询退款状态（需商户 HMAC 头）")
    public R<RefundResponse> getRefund(
            @PathVariable String refundId,
            HttpServletRequest httpRequest) {
        String merchantId = (String) httpRequest.getAttribute(MerchantSignatureInterceptor.ATTR_MERCHANT_ID);
        return R.ok(refundService.getRefund(merchantId, refundId));
    }
}
