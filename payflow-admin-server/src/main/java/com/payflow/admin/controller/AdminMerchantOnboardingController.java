package com.payflow.admin.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.payflow.admin.dto.onboarding.MerchantApplicationRejectRequest;
import com.payflow.admin.security.RequirePermission;
import com.payflow.admin.entity.MerchantApplicationEntity;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.entity.AdminUser;
import com.payflow.admin.service.MerchantOnboardingService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 商户进件（KYB）管理端接口。
 */
@RestController
@RequestMapping("/api/v1/admin/onboarding")
@RequiredArgsConstructor
public class AdminMerchantOnboardingController {

    private final MerchantOnboardingService merchantOnboardingService;
    private final AdminUserMapper adminUserMapper;

    @GetMapping("/applications")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        int resolvedSize = pageSize != null ? pageSize : (size != null ? size : 20);
        IPage<MerchantApplicationEntity> result = merchantOnboardingService.pageApplications(
                page, resolvedSize, status, keyword);
        Map<String, Object> data = new HashMap<>();
        data.put("total", result.getTotal());
        data.put("page", page);
        data.put("pageSize", resolvedSize);
        data.put("list", result.getRecords());
        return ok(data);
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        return ok(merchantOnboardingService.getDetail(id));
    }

    @RequirePermission("onboarding:approve")
    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<Map<String, Object>> approve(@PathVariable Long id, HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        Long approverId = resolveUserId(username);
        merchantOnboardingService.approve(id, username, approverId, clientIp(request));
        Map<String, Object> data = new HashMap<>();
        data.put("message", "审批通过，商户可通过自助查询页获取密钥");
        return ok(data);
    }

    @RequirePermission("onboarding:reject")
    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<Map<String, Object>> reject(@PathVariable Long id,
                                                      @Valid @RequestBody MerchantApplicationRejectRequest body,
                                                      HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        Long approverId = resolveUserId(username);
        merchantOnboardingService.reject(id, body, username, approverId, clientIp(request));
        return ok(Map.of("message", "已拒绝该申请"));
    }

    private Long resolveUserId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        AdminUser user = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username));
        return user != null ? user.getId() : null;
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
