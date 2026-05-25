package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.*;
import com.payflow.admin.mapper.*;
import com.payflow.admin.service.SysMenuService;
import com.payflow.admin.service.guard.ResourceDeleteGuardService;
import com.payflow.admin.service.guard.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper sysMenuMapper;
    private final AdminUserMapper adminUserMapper;
    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleMapper sysRoleMapper;
    private final ResourceDeleteGuardService resourceDeleteGuardService;

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
        validateButtonMenu(menu, null);
        sysMenuMapper.insert(menu);
        return menu;
    }

    @Override
    public SysMenu update(SysMenu menu) {
        validateButtonMenu(menu, menu.getId());
        sysMenuMapper.updateById(menu);
        return menu;
    }

    /**
     * BUTTON 类型必须填写唯一 perm_code。
     */
    private void validateButtonMenu(SysMenu menu, Long excludeId) {
        if (!"BUTTON".equals(menu.getMenuType())) {
            return;
        }
        if (!StringUtils.hasText(menu.getPermCode())) {
            throw new IllegalArgumentException("按钮类型菜单必须填写权限码 perm_code");
        }
        String permCode = menu.getPermCode().trim();
        menu.setPermCode(permCode);
        if (!permCode.matches("^[a-z][a-z0-9_]*(:[a-z][a-z0-9_]*){1,2}$")) {
            throw new IllegalArgumentException("权限码格式无效，示例：refund:approve 或 order:export");
        }
        LambdaQueryWrapper<SysMenu> q = new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getPermCode, permCode);
        if (excludeId != null) {
            q.ne(SysMenu::getId, excludeId);
        }
        Long count = sysMenuMapper.selectCount(q);
        if (count != null && count > 0) {
            throw new IllegalArgumentException("权限码已存在: " + permCode);
        }
    }

    @Override
    public void delete(Long id) {
        resourceDeleteGuardService.assertDeletable(ResourceType.SYS_MENU, id);
        sysMenuMapper.deleteById(id);
    }

    @Override
    public List<SysMenu> getMenuTree() {
        List<SysMenu> allMenus = list();
        return buildTree(allMenus);
    }

    @Override
    public List<SysMenu> getMenusByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return Collections.emptyList();
        }
        String normalized = username.trim();

        SysUser sysUser = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, normalized)
                        .eq(SysUser::getStatus, "ACTIVE")
        );
        if (sysUser != null) {
            return menusForSysUser(sysUser.getId());
        }

        AdminUser adminUser = adminUserMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>()
                        .eq(AdminUser::getUsername, normalized)
                        .eq(AdminUser::getStatus, "ACTIVE")
        );
        if (adminUser == null || !StringUtils.hasText(adminUser.getRole())) {
            return Collections.emptyList();
        }

        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, adminUser.getRole().trim())
                        .eq(SysRole::getStatus, "ACTIVE")
        );
        if (role == null) {
            return Collections.emptyList();
        }
        return menusForRoleIds(List.of(role.getId()));
    }

    private List<SysMenu> menusForSysUser(Long sysUserId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, sysUserId)
        );
        if (userRoles.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return menusForRoleIds(roleIds);
    }

    private List<SysMenu> menusForRoleIds(List<Long> roleIds) {
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
        List<SysMenu> menus = sysMenuMapper.selectBatchIds(menuIds);
        menus.sort((a, b) -> {
            if (a.getSortOrder() == null) {
                return 1;
            }
            if (b.getSortOrder() == null) {
                return -1;
            }
            return a.getSortOrder().compareTo(b.getSortOrder());
        });
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
