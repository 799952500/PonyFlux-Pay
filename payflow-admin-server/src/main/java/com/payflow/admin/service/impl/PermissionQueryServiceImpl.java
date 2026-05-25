package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.admin.config.PermissionProperties;
import com.payflow.admin.entity.AdminUser;
import com.payflow.admin.entity.SysMenu;
import com.payflow.admin.entity.SysRole;
import com.payflow.admin.entity.SysRoleMenu;
import com.payflow.admin.entity.SysUser;
import com.payflow.admin.entity.SysUserRole;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.mapper.SysMenuMapper;
import com.payflow.admin.mapper.SysRoleMapper;
import com.payflow.admin.mapper.SysRoleMenuMapper;
import com.payflow.admin.mapper.SysUserMapper;
import com.payflow.admin.mapper.SysUserRoleMapper;
import com.payflow.admin.service.PermissionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionQueryServiceImpl implements PermissionQueryService {

    private static final String CACHE_KEY_PREFIX = "admin:perms:";

    private final AdminUserMapper adminUserMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysMenuMapper sysMenuMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final PermissionProperties permissionProperties;
    private final ObjectMapper objectMapper;

    @Override
    public Set<String> getPermCodesByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Collections.emptySet();
        }
        String cacheKey = CACHE_KEY_PREFIX + username;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                List<String> list = objectMapper.readValue(cached, new TypeReference<>() {});
                return new HashSet<>(list);
            }
        } catch (Exception e) {
            log.warn("读取权限缓存失败，将回源数据库: username={}", username, e);
        }

        Set<String> codes = loadPermCodesFromDb(username);
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(codes),
                    Duration.ofSeconds(permissionProperties.getCacheTtlSeconds()));
        } catch (Exception e) {
            log.warn("写入权限缓存失败: username={}", username, e);
        }
        return codes;
    }

    @Override
    public void evictByRoleId(Long roleId) {
        if (roleId == null) {
            return;
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        for (SysUserRole ur : userRoles) {
            SysUser user = sysUserMapper.selectById(ur.getUserId());
            if (user != null && StringUtils.hasText(user.getUsername())) {
                evictByUsername(user.getUsername());
            }
        }
    }

    @Override
    public void evictByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return;
        }
        try {
            stringRedisTemplate.delete(CACHE_KEY_PREFIX + username);
        } catch (Exception e) {
            log.warn("清除权限缓存失败: username={}", username, e);
        }
    }

    private Set<String> loadPermCodesFromDb(String username) {
        List<Long> roleIds = resolveRoleIdsForUsername(username);
        if (roleIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus.isEmpty()) {
            return Collections.emptySet();
        }

        List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).distinct().collect(Collectors.toList());
        List<SysMenu> menus = sysMenuMapper.selectBatchIds(menuIds);
        return menus.stream()
                .filter(m -> "BUTTON".equals(m.getMenuType()))
                .map(SysMenu::getPermCode)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private List<Long> resolveRoleIdsForUsername(String username) {
        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getStatus, "ACTIVE"));
        if (sysUser != null) {
            return sysUserRoleMapper.selectList(
                            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, sysUser.getId()))
                    .stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());
        }

        AdminUser adminUser = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, username)
                        .eq(AdminUser::getStatus, "ACTIVE"));
        if (adminUser == null || !StringUtils.hasText(adminUser.getRole())) {
            return List.of();
        }

        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, adminUser.getRole().trim())
                        .eq(SysRole::getStatus, "ACTIVE"));
        if (role == null) {
            return List.of();
        }
        return List.of(role.getId());
    }
}
