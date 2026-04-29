package com.payflow.admin.controller;

import com.payflow.admin.dto.LoginRequest;
import com.payflow.admin.dto.LoginResponse;
import com.payflow.admin.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /**
     * 管理员登录
     *
     * @param request 登录请求（包含用户名、密码）
     * @return 登录成功返回 Token 及用户信息
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse data = adminAuthService.login(request);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", data));
    }
}
