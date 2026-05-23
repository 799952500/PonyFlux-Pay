package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.config.JwtProperties;
import com.payflow.admin.dto.AdminUiPreferencesDto;
import com.payflow.admin.dto.LoginRequest;
import com.payflow.admin.dto.LoginResponse;
import com.payflow.admin.dto.MerchantScopeDTO;
import com.payflow.admin.dto.UpdateAdminUiPreferencesRequest;
import com.payflow.admin.entity.AdminUser;
import com.payflow.admin.entity.SysMenu;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.service.AdminAuthService;
import com.payflow.admin.service.AdminMerchantScopeService;
import com.payflow.admin.service.AdminUserPreferenceService;
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
    private final AdminMerchantScopeService adminMerchantScopeService;
    private final AdminUserPreferenceService adminUserPreferenceService;

    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        request.setUsername(username);

        String ip = clientIp(httpRequest);
        loginProtectionService.assertNotLocked(username);

        if (loginProtectionService.isCaptchaRequired(username)) {
            if (!StringUtils.hasText(request.getCaptchaId()) || !StringUtils.hasText(request.getCaptchaAnswer())) {
                throw new IllegalArgumentException("密码错误后需输入验证码");
            }
            captchaService.validateAndConsume(request.getCaptchaId(), request.getCaptchaAnswer().trim());
        }

        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, username)
                        .eq(AdminUser::getStatus, "ACTIVE")
        );

        if (user == null) {
            loginProtectionService.recordFailure(username);
            auditLogService.recordLogin(username, false, ip);
            throw new IllegalArgumentException("Invalid username or password");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            loginProtectionService.recordFailure(username);
            auditLogService.recordLogin(username, false, ip);
            throw new IllegalArgumentException("Invalid username or password");
        }

        loginProtectionService.clearFailures(username);

        String token = jwtUtils.generateToken(user.getUsername(), user.getRole(), user.getDataMerchantIds());

        LocalDateTime expireTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(System.currentTimeMillis() + jwtProperties.getExpiration()),
                ZoneId.of("+08:00")
        );

        List<SysMenu> menus = sysMenuService.getMenusByUsername(user.getUsername());
        MerchantScopeDTO merchantScope = adminMerchantScopeService.resolve(user);

        auditLogService.recordLogin(user.getUsername(), true, ip);

        return buildLoginResponse(user, token, expireTime, menus, merchantScope);
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
        MerchantScopeDTO merchantScope = adminMerchantScopeService.resolve(user);
        LocalDateTime expireTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(System.currentTimeMillis() + jwtProperties.getExpiration()),
                ZoneId.of("+08:00"));
        return buildLoginResponse(user, null, expireTime, menus, merchantScope);
    }

    @Override
    public AdminUiPreferencesDto updateUiPreferences(HttpServletRequest httpRequest,
                                                     UpdateAdminUiPreferencesRequest request) {
        String username = requireUsername(httpRequest);
        return adminUserPreferenceService.updateCurrentUser(username, request);
    }

    private LoginResponse buildLoginResponse(AdminUser user, String token, LocalDateTime expireTime,
                                             List<SysMenu> menus, MerchantScopeDTO merchantScope) {
        return LoginResponse.builder()
                .token(token)
                .adminId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .role(user.getRole())
                .platformAdmin(merchantScope.isPlatformAdmin())
                .scopeMode(merchantScope.getScopeMode())
                .authorizedMerchantIds(merchantScope.getAuthorizedMerchantIds())
                .expireTime(expireTime)
                .menus(menus)
                .uiPreferences(adminUserPreferenceService.fromUser(user))
                .build();
    }

    private static String requireUsername(HttpServletRequest httpRequest) {
        Object u = httpRequest.getAttribute("username");
        if (u == null || !StringUtils.hasText(u.toString())) {
            throw new IllegalStateException("未登录或令牌无效");
        }
        return u.toString();
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
