package com.payflow.admin.controller;

import com.payflow.admin.service.CashierPaymentConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 供收银台服务拉取商户支付方式配置（需内部令牌）。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/internal/cashier")
@RequiredArgsConstructor
public class InternalCashierPaymentController {

    private final CashierPaymentConfigService cashierPaymentConfigService;

    /**
     * 按商户与订单支付渠道返回可用支付方式及终端范围。
     */
    @GetMapping("/payment-methods")
    public ResponseEntity<Map<String, Object>> paymentMethods(
            @RequestParam String merchantId,
            @RequestParam String orderChannel) {
        List<Map<String, Object>> list = cashierPaymentConfigService.listPaymentMethodsForCashier(merchantId, orderChannel);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", list
        ));
    }
}
