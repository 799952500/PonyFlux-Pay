package com.payflow.admin.controller;

import com.payflow.admin.dto.AdminUiPreferencesDto;
import com.payflow.admin.dto.LoginRequest;
import com.payflow.admin.dto.LoginResponse;
import com.payflow.admin.dto.UpdateAdminUiPreferencesRequest;
import com.payflow.admin.service.AdminAuthService;
import com.payflow.admin.service.CaptchaService;
import com.payflow.admin.service.LoginProtectionService;
import com.payflow.admin.service.PermissionQueryService;
import com.payflow.admin.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * 管理员身份认证 Controller
  * @author Lucas
 */
@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminAuthService adminAuthService;
    private final CaptchaService captchaService;
    private final LoginProtectionService loginProtectionService;
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final PermissionQueryService permissionQueryService;

    /**
     * 签发算术验证码（密码错误后登录需携带 captchaId 与 captchaAnswer）。
     */
    @Operation(summary = "获取验证码")
    @GetMapping("/captcha")
    public ResponseEntity<Map<String, Object>> captcha() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("code", 0, "message", "success", "data", captchaService.issue()));
    }

    /**
     * 查询指定用户名当前是否需要验证码（首次登录为 false，已有密码错误记录为 true）。
     */
    @Operation(summary = "查询登录是否需要验证码")
    @GetMapping("/captcha-required")
    public ResponseEntity<Map<String, Object>> captchaRequired(
            @RequestParam(value = "username", defaultValue = "") String username) {
        String normalized = username == null ? "" : username.trim();
        boolean required = loginProtectionService.isCaptchaRequired(normalized);
        Map<String, Object> data = Map.of(
                "required", required,
                "failureCount", loginProtectionService.getFailureCount(normalized)
        );
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of("code", 0, "message", "success", "data", data));
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
     * 刷新当前用户按钮权限码列表。
     */
    @Operation(summary = "获取当前用户按钮权限")
    @GetMapping("/permissions")
    public ResponseEntity<Map<String, Object>> permissions(HttpServletRequest httpRequest) {
        Object usernameAttr = httpRequest.getAttribute("username");
        String username = usernameAttr != null ? usernameAttr.toString() : "";
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", Map.of("permissions", new ArrayList<>(permissionQueryService.getPermCodesByUsername(username)))));
    }

    /**
     * 更新当前登录用户的 UI 外观偏好（主题、表格密度、侧栏状态）。
     */
    @Operation(summary = "更新当前用户 UI 偏好")
    @PutMapping("/ui-preferences")
    public ResponseEntity<Map<String, Object>> updateUiPreferences(
            HttpServletRequest httpRequest,
            @RequestBody UpdateAdminUiPreferencesRequest request) {
        AdminUiPreferencesDto data = adminAuthService.updateUiPreferences(httpRequest, request);
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
            } catch (Exception ex) {
                log.warn("登出写入 JWT 黑名单失败: {}", ex.getMessage());
                return ResponseEntity.status(503).body(Map.of(
                        "code", 503,
                        "message", "登出失败，请稍后重试",
                        "data", null));
            }
        }
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", null));
    }
}
