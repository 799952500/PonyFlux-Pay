package com.payflow.admin.controller;

import com.payflow.admin.dto.onboarding.MerchantApplicationResultRequest;
import com.payflow.admin.dto.onboarding.MerchantApplicationSubmitRequest;
import com.payflow.admin.service.MerchantOnboardingService;
import jakarta.servlet.http.HttpServletRequest;
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
 * 收银台等服务调用的入驻内部接口（X-Payflow-Internal-Token）。
 */
@RestController
@RequestMapping("/api/v1/internal/onboarding")
@RequiredArgsConstructor
public class InternalOnboardingController {

    private final MerchantOnboardingService merchantOnboardingService;

    @PostMapping("/applications")
    public ResponseEntity<Map<String, Object>> submit(@Valid @RequestBody MerchantApplicationSubmitRequest request,
                                                      HttpServletRequest httpRequest) {
        var data = merchantOnboardingService.submit(request, clientIp(httpRequest));
        return ok(data);
    }

    @PostMapping("/result")
    public ResponseEntity<Map<String, Object>> queryResult(@Valid @RequestBody MerchantApplicationResultRequest request,
                                                            HttpServletRequest httpRequest) {
        var data = merchantOnboardingService.queryResult(request, clientIp(httpRequest));
        return ok(data);
    }

    private static ResponseEntity<Map<String, Object>> ok(Object data) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("message", "success");
        resp.put("data", data);
        return ResponseEntity.ok(resp);
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
