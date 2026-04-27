package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.*;
import com.payflow.admin.mapper.*;
import com.payflow.admin.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final AdminUserMapper adminUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleMapper sysRoleMapper;

    @Override
    public List<SysMenu> list() {
        return sysMenuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder)
        );
    }

    @Override
    public SysMenu getById(Long id) {
        return sysMenuMapper.selectById(id);
    }

    @Override
    public SysMenu create(SysMenu menu) {
        sysMenuMapper.insert(menu);
        return menu;
    }

    @Override
    public SysMenu update(SysMenu menu) {
        sysMenuMapper.updateById(menu);
        return menu;
    }

    @Override
    public void delete(Long id) {
        sysMenuMapper.deleteById(id);
    }

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> allMenus = list();
        return buildTree(allMenus);
    }

    @Override
    public List<SysMenu> getMenusByUsername(String username) {
        // 1. 查用户
        AdminUser user = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username)
        );
        if (user == null) {
            return Collections.emptyList();
        }

        // 2. 查用户角色
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, user.getId())
        );
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());

        // 3. 查角色关联的菜单ID
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds)
        );
        if (roleMenus.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> menuIds = roleMenus.stream()
                .map(SysRoleMenu::getMenuId)
                .distinct()
                .collect(Collectors.toList());

        // 4. 查菜单
        List<SysMenu> menus = sysMenuMapper.selectBatchIds(menuIds);
        menus.sort((a, b) -> {
            if (a.getSortOrder() == null) return 1;
            if (b.getSortOrder() == null) return -1;
            return a.getSortOrder().compareTo(b.getSortOrder());
        });

        // 5. 构建树形（确保父菜单也包含）
        return buildTree(ensureParents(menus));
    }

    /**
     * 确保子菜单的父菜单也在列表中
     */
    private List<SysMenu> ensureParents(List<SysMenu> menus) {
        if (menus.isEmpty()) {
            return menus;
        }
        Map<Long, SysMenu> menuMap = menus.stream()
                .collect(Collectors.toMap(SysMenu::getId, m -> m));

        boolean changed = true;
        while (changed) {
            changed = false;
            for (SysMenu menu : new ArrayList<>(menuMap.values())) {
                if (menu.getParentId() != null && !menuMap.containsKey(menu.getParentId())) {
                    SysMenu parent = sysMenuMapper.selectById(menu.getParentId());
                    if (parent != null) {
                        menuMap.put(parent.getId(), parent);
                        changed = true;
                    }
                }
            }
        }
        return new ArrayList<>(menuMap.values());
    }

    /**
     * 构建菜单树
     */
    private List<SysMenu> buildTree(List<SysMenu> allMenus) {
        Map<Long, List<SysMenu>> childrenMap = allMenus.stream()
                .filter(m -> m.getParentId() != null)
                .collect(Collectors.groupingBy(SysMenu::getParentId));

        List<SysMenu> roots = allMenus.stream()
                .filter(m -> m.getParentId() == null)
                .collect(Collectors.toList());

        for (SysMenu root : roots) {
            fillChildren(root, childrenMap);
        }
        return roots;
    }

    private void fillChildren(SysMenu menu, Map<Long, List<SysMenu>> childrenMap) {
        List<SysMenu> children = childrenMap.get(menu.getId());
        if (children != null) {
            menu.setChildren(children);
            for (SysMenu child : children) {
                fillChildren(child, childrenMap);
            }
        }
    }
}
