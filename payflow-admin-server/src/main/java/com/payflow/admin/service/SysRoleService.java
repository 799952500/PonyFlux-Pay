package com.payflow.admin.service;

import com.payflow.admin.entity.SysMenu;
import com.payflow.admin.entity.SysRole;

import java.util.List;
/**
 * @author Lucas
 */

public interface SysRoleService {

    List<SysRole> list();

    SysRole getById(Long id);

    SysRole create(SysRole role);

    SysRole update(SysRole role);

    void delete(Long id);

    List<SysMenu> getMenusByRoleId(Long roleId);

    void assignMenus(Long roleId, List<Long> menuIds);
}
