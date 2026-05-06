package com.payflow.admin.controller;

import com.payflow.admin.dto.LoginRequest;
import com.payflow.admin.dto.LoginResponse;
import com.payflow.admin.service.AdminAuthService;
import com.payflow.admin.service.CaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员身份认证 Controller
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminAuthService adminAuthService;
    private final CaptchaService captchaService;

    /**
     * 签发算术验证码（登录前必须先调用，提交登录时携带 captchaId 与 captchaAnswer）。
     */
    @GetMapping("/captcha")
    public ResponseEntity<Map<String, Object>> captcha() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("code", 0, "message", "success", "data", captchaService.issue()));
    }

    /**
     * 管理员登录
     *
     * @param request 登录请求（包含用户名、密码）
     * @return 登录成功返回 Token 及用户信息
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        LoginResponse data = adminAuthService.login(request, httpRequest);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    /**
     * 当前登录用户信息（需携带 JWT；不含密码）。
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(HttpServletRequest httpRequest) {
        LoginResponse data = adminAuthService.profile(httpRequest);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
