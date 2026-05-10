package com.payflow.admin.controller;

import com.payflow.admin.dto.LoginRequest;
import com.payflow.admin.dto.LoginResponse;
import com.payflow.admin.service.AdminAuthService;
import com.payflow.admin.service.CaptchaService;
import com.payflow.admin.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Date;
import java.util.Map;

/**
 * 管理员身份认证 Controller
  * @author Lucas
 */
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminAuthService adminAuthService;
    private final CaptchaService captchaService;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 签发算术验证码（登录前必须先调用，提交登录时携带 captchaId 与 captchaAnswer）。
     */
    @Operation(summary = "获取验证码")
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
    @Operation(summary = "管理员登录")
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
    @Operation(summary = "获取当前用户信息")
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(HttpServletRequest httpRequest) {
        LoginResponse data = adminAuthService.profile(httpRequest);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }

    /**
     * 管理员登出，将当前 JWT 加入黑名单。
     */
    @Operation(summary = "管理员登出")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                String jti = jwtUtils.getJti(token);
                Date expiration = jwtUtils.getExpiration(token);
                long ttlSeconds = Math.max(1, (expiration.getTime() - System.currentTimeMillis()) / 1000);
                stringRedisTemplate.opsForValue()
                        .set("jwt:blacklist:" + jti, "logout", Duration.ofSeconds(ttlSeconds));
            } catch (Exception ignored) {
            }
        }
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", null));
    }
}
