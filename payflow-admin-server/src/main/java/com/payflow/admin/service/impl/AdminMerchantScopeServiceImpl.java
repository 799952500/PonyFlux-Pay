package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.dto.MerchantScopeDTO;
import com.payflow.admin.entity.AdminUser;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.service.AdminMerchantScopeService;
import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminMerchantScopeServiceImpl implements AdminMerchantScopeService {

    private static final String SUPER_ADMIN = "SUPER_ADMIN";
    private static final int CROSS_MERCHANT_DENIED = 6101;

    private final AdminUserMapper adminUserMapper;

    @Override
    public MerchantScopeDTO resolve(AdminUser user) {
        if (user == null) {
            return MerchantScopeDTO.builder()
                    .platformAdmin(false)
                    .scopeMode("NONE")
                    .authorizedMerchantIds(List.of())
                    .build();
        }
        boolean platformAdmin = SUPER_ADMIN.equalsIgnoreCase(nullToEmpty(user.getRole()))
                && !StringUtils.hasText(user.getDataMerchantIds());
        List<String> merchantIds = parseMerchantIds(user.getDataMerchantIds());
        return MerchantScopeDTO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .platformAdmin(platformAdmin)
                .scopeMode(platformAdmin ? "PLATFORM" : "MERCHANT")
                .authorizedMerchantIds(merchantIds)
                .build();
    }

    @Override
    public MerchantScopeDTO resolve(HttpServletRequest request) {
        if (request == null) {
            return resolve((AdminUser) null);
        }
        Object username = request.getAttribute("username");
        if (username == null || !StringUtils.hasText(username.toString())) {
            return resolve((AdminUser) null);
        }
        AdminUser user = adminUserMapper.selectOne(new LambdaQueryWrapper<AdminUser>()
                .eq(AdminUser::getUsername, username.toString())
                .eq(AdminUser::getStatus, "ACTIVE"));
        return resolve(user);
    }

    @Override
    public List<String> intersectAuthorizedMerchants(MerchantScopeDTO scope, Collection<String> requestedMerchantIds) {
        if (scope == null) {
            return List.of();
        }
        List<String> requested = normalize(requestedMerchantIds);
        if (scope.isPlatformAdmin()) {
            return requested;
        }
        List<String> authorized = normalize(scope.getAuthorizedMerchantIds());
        if (requested.isEmpty()) {
            return authorized;
        }
        Set<String> requestedSet = new LinkedHashSet<>(requested);
        return authorized.stream().filter(requestedSet::contains).toList();
    }

    @Override
    public boolean canAccessMerchant(MerchantScopeDTO scope, String merchantId) {
        if (!StringUtils.hasText(merchantId)) {
            return scope != null && scope.isPlatformAdmin();
        }
        if (scope == null) {
            return false;
        }
        if (scope.isPlatformAdmin()) {
            return true;
        }
        return normalize(scope.getAuthorizedMerchantIds()).contains(merchantId.trim());
    }

    @Override
    public void assertCanAccessMerchant(MerchantScopeDTO scope, String merchantId) {
        if (!canAccessMerchant(scope, merchantId)) {
            throw new BizException(CROSS_MERCHANT_DENIED, "无权访问该资源");
        }
    }

    private static List<String> parseMerchantIds(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        String[] parts = raw.split(",");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String value = part.trim();
            if (StringUtils.hasText(value)) {
                result.add(value);
            }
        }
        return result.stream().distinct().toList();
    }

    private static List<String> normalize(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }
}
