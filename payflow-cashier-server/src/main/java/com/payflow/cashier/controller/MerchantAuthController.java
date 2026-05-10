package com.payflow.cashier.controller;

import com.payflow.cashier.config.PayflowProperties;
import com.payflow.cashier.dto.LoginRequest;
import com.payflow.cashier.dto.LoginResponse;
import com.payflow.cashier.dto.MerchantLoginApiVO;
import com.payflow.cashier.entity.Merchant;
import com.payflow.cashier.exception.R;
import com.payflow.cashier.service.AuthService;
import com.payflow.cashier.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 商户认证接口（收银台前端登录）。
 *
 * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class MerchantAuthController {

    private final AuthService authService;
    private final PayflowProperties properties;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 商户登录，返回前端约定的 token + merchantInfo 结构。
     *
     * @param request 登录请求
     * @return 统一包装成功响应
     */
    @PostMapping("/login")
    public R<MerchantLoginApiVO> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse inner = authService.login(request);
        MerchantLoginApiVO vo = MerchantLoginApiVO.builder()
                .token(inner.getToken())
                .merchantInfo(MerchantLoginApiVO.MerchantInfo.builder()
                        .merchantId(inner.getMerchantId())
                        .merchantName(inner.getMerchantName())
                        .merchantType("ENTERPRISE")
                        .status(toFrontendMerchantStatus(inner.getMerchantStatus()))
                        .build())
                .build();
        return R.ok(vo);
    }

    /**
     * 商户登出，将当前 JWT 加入黑名单。
     */
    @PostMapping("/logout")
    public R<Void> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            JwtUtils.TokenClaims claims = JwtUtils.parseClaims(properties.getJwt().getSecret(), token);
            if (claims != null && claims.jti() != null && claims.expiration() != null) {
                long ttlSeconds = Math.max(1, (claims.expiration().getTime() - System.currentTimeMillis()) / 1000);
                stringRedisTemplate.opsForValue()
                        .set("jwt:blacklist:" + claims.jti(), "logout", Duration.ofSeconds(ttlSeconds));
            }
        }
        return R.ok();
    }

    /**
     * 将数据库商户状态映射为前端枚举（ACTIVE/SUSPENDED/CLOSED）。
     *
     * @param dbStatus 数据库状态
     * @return 前端状态
     */
    private String toFrontendMerchantStatus(String dbStatus) {
        if (dbStatus == null || dbStatus.isBlank()) {
            return "CLOSED";
        }
        if (Merchant.STATUS_ACTIVE.equals(dbStatus)) {
            return "ACTIVE";
        }
        if (Merchant.STATUS_SUSPENDED.equals(dbStatus)) {
            return "SUSPENDED";
        }
        return "CLOSED";
    }
}
