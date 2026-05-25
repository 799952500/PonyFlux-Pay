package com.payflow.admin.service;

import java.util.Set;

/**
 * 查询用户按钮权限码（perm_code）。
 */
public interface PermissionQueryService {

    /**
     * 获取用户拥有的全部 perm_code（去重）。
     */
    Set<String> getPermCodesByUsername(String username);

    /**
     * 角色菜单变更后，清除该角色下所有用户的权限缓存。
     */
    void evictByRoleId(Long roleId);

    /**
     * 清除指定用户权限缓存。
     */
    void evictByUsername(String username);
}
