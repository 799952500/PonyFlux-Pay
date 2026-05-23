package com.payflow.cashier.controller;

import com.payflow.cashier.dto.MerchantProvisionRequest;
import com.payflow.cashier.service.MerchantProvisionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理端内部调用：开通/更新收银台商户（受 InternalApiTokenFilter 保护）。
 */
@RestController
@RequestMapping("/api/v1/internal/merchants")
@RequiredArgsConstructor
public class InternalMerchantProvisionController {

    private final MerchantProvisionService merchantProvisionService;

    @PostMapping("/provision")
    public ResponseEntity<Map<String, Object>> provision(@Valid @RequestBody MerchantProvisionRequest request) {
        merchantProvisionService.provision(request);
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", Map.of("merchantId", request.getMerchantId()));
        return ResponseEntity.ok(resp);
    }
}
