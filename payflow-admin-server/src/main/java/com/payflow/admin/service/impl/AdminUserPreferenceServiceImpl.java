package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.dto.AdminUiPreferencesDto;
import com.payflow.admin.dto.UpdateAdminUiPreferencesRequest;
import com.payflow.admin.entity.AdminUser;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.service.AdminUserPreferenceService;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminUserPreferenceServiceImpl implements AdminUserPreferenceService {

    private static final String DEFAULT_THEME = "mint";
    private static final String DEFAULT_DENSITY = "standard";
    private static final Set<String> VALID_THEMES = Set.of("mint", "ocean", "violet", "dark");
    private static final Set<String> VALID_DENSITIES = Set.of("standard", "compact");

    private final AdminUserMapper adminUserMapper;

    @Override
    public AdminUiPreferencesDto fromUser(AdminUser user) {
        if (user == null) {
            return defaults();
        }
        return AdminUiPreferencesDto.builder()
                .themeKey(normalizeTheme(user.getUiTheme()))
                .tableDensity(normalizeDensity(user.getUiTableDensity()))
                .sidebarCollapsed(Boolean.TRUE.equals(user.getUiSidebarCollapsed()))
                .build();
    }

    @Override
    @Transactional
    public AdminUiPreferencesDto updateCurrentUser(String username, UpdateAdminUiPreferencesRequest request) {
        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, username)
                        .eq(AdminUser::getStatus, "ACTIVE"));
        if (user == null) {
            throw new BizException(4004, "用户不存在或已禁用");
        }
        if (request == null) {
            return fromUser(user);
        }
        if (StringUtils.hasText(request.getThemeKey())) {
            user.setUiTheme(normalizeTheme(request.getThemeKey()));
        }
        if (StringUtils.hasText(request.getTableDensity())) {
            user.setUiTableDensity(normalizeDensity(request.getTableDensity()));
        }
        if (request.getSidebarCollapsed() != null) {
            user.setUiSidebarCollapsed(request.getSidebarCollapsed());
        }
        user.setUpdatedAt(LocalDateTime.now());
        adminUserMapper.updateById(user);
        return fromUser(user);
    }

    private static AdminUiPreferencesDto defaults() {
        return AdminUiPreferencesDto.builder()
                .themeKey(DEFAULT_THEME)
                .tableDensity(DEFAULT_DENSITY)
                .sidebarCollapsed(false)
                .build();
    }

    private static String normalizeTheme(String raw) {
        if (raw != null && VALID_THEMES.contains(raw.trim())) {
            return raw.trim();
        }
        return DEFAULT_THEME;
    }

    private static String normalizeDensity(String raw) {
        if (raw != null && VALID_DENSITIES.contains(raw.trim())) {
            return raw.trim();
        }
        return DEFAULT_DENSITY;
    }
}
