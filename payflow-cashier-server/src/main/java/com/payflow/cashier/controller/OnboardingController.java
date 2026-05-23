package com.payflow.cashier.controller;

import com.payflow.cashier.client.AdminOnboardingClient;
import com.payflow.cashier.service.OnboardingPublicRateLimitService;
import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 公网商户入驻申请（无需 HMAC / JWT）。
 */
@RestController
@RequestMapping("/api/v1/cashier/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final AdminOnboardingClient adminOnboardingClient;
    private final OnboardingPublicRateLimitService rateLimitService;

    @PostMapping("/applications")
    public ResponseEntity<Map<String, Object>> submit(@Valid @RequestBody SubmitBody body,
                                                        HttpServletRequest request) {
        rateLimitService.assertSubmitAllowed(clientIp(request));
        Map<String, Object> payload = new HashMap<>();
        payload.put("merchantName", body.getMerchantName());
        payload.put("contactName", body.getContactName());
        payload.put("contactPhone", body.getContactPhone());
        payload.put("contactEmail", body.getContactEmail());
        payload.put("bizLicenseNo", body.getBizLicenseNo());
        payload.put("websiteUrl", body.getWebsiteUrl());
        payload.put("businessScope", body.getBusinessScope());
        payload.put("remark", body.getRemark());
        try {
            return ok(adminOnboardingClient.submitApplication(payload));
        } catch (BizException ex) {
            throw ex;
        } catch (IllegalArgumentException ex) {
            throw new BizException(6208, ex.getMessage());
        } catch (IllegalStateException ex) {
            throw new BizException(5000, ex.getMessage());
        }
    }

    @PostMapping("/result")
    public ResponseEntity<Map<String, Object>> queryResult(@Valid @RequestBody ResultBody body,
                                                            HttpServletRequest request) {
        Map<String, Object> payload = Map.of(
                "applicationNo", body.getApplicationNo(),
                "contact", body.getContact());
        return ok(adminOnboardingClient.queryResult(payload));
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

    @Data
    public static class SubmitBody {
        @NotBlank
        @Size(max = 128)
        private String merchantName;
        @NotBlank
        @Size(max = 64)
        private String contactName;
        @NotBlank
        @Pattern(regexp = "^1\\d{10}$")
        private String contactPhone;
        @NotBlank
        @Email
        @Size(max = 128)
        private String contactEmail;
        @Size(max = 64)
        private String bizLicenseNo;
        @Size(max = 256)
        private String websiteUrl;
        @Size(max = 256)
        private String businessScope;
        @Size(max = 512)
        private String remark;
    }

    @Data
    public static class ResultBody {
        @NotBlank
        @Size(max = 64)
        private String applicationNo;
        @NotBlank
        @Size(max = 128)
        private String contact;
    }
}
