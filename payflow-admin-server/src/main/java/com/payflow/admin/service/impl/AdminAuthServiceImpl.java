package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.config.JwtProperties;
import com.payflow.admin.dto.LoginRequest;
import com.payflow.admin.dto.LoginResponse;
import com.payflow.admin.entity.AdminUser;
import com.payflow.admin.entity.SysMenu;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.service.AdminAuthService;
import com.payflow.admin.service.AuditLogService;
import com.payflow.admin.service.CaptchaService;
import com.payflow.admin.service.LoginProtectionService;
import com.payflow.admin.service.SysMenuService;
import com.payflow.admin.util.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final SysMenuService sysMenuService;
    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final CaptchaService captchaService;
    private final LoginProtectionService loginProtectionService;
    private final AuditLogService auditLogService;

    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip = clientIp(httpRequest);
        loginProtectionService.assertNotLocked(request.getUsername());

        captchaService.validateAndConsume(request.getCaptchaId(), request.getCaptchaAnswer());

        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, request.getUsername())
                        .eq(AdminUser::getStatus, "ACTIVE")
        );

        if (user == null) {
            loginProtectionService.recordFailure(request.getUsername());
            auditLogService.recordLogin(request.getUsername(), false, ip);
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginProtectionService.recordFailure(request.getUsername());
            auditLogService.recordLogin(request.getUsername(), false, ip);
            throw new IllegalArgumentException("Invalid username or password");
        }

        loginProtectionService.clearFailures(request.getUsername());

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getDataMerchantIds());

        LocalDateTime expireTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(System.currentTimeMillis() + jwtProperties.getExpiration()),
                ZoneId.of("+08:00")
        );

        List<SysMenu> menus = sysMenuService.getMenusByUsername(user.getUsername());

        auditLogService.recordLogin(user.getUsername(), true, ip);

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole())
                .expireTime(expireTime)
                .menus(menus)
                .build();
    }

    @Override
    public LoginResponse profile(HttpServletRequest httpRequest) {
        Object u = httpRequest.getAttribute("username");
        if (u == null || !StringUtils.hasText(u.toString())) {
            throw new IllegalStateException("未登录或令牌无效");
        }
        String username = u.toString();
        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, username)
                        .eq(AdminUser::getStatus, "ACTIVE"));
        if (user == null) {
            throw new IllegalArgumentException("用户不存在或已禁用");
        }
        List<SysMenu> menus = sysMenuService.getMenusByUsername(username);
        LocalDateTime expireTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(System.currentTimeMillis() + jwtProperties.getExpiration()),
                ZoneId.of("+08:00"));
        return LoginResponse.builder()
                .token(null)
                .username(user.getUsername())
                .role(user.getRole())
                .expireTime(expireTime)
                .menus(menus)
                .build();
    }

    private static String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
