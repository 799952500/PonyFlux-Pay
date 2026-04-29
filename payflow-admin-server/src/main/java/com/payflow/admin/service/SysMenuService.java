package com.payflow.admin.service;

import com.payflow.admin.entity.SysMenu;

import java.util.List;
/**
 * @author Lucas
 */

public interface SysMenuService {

    List<SysMenu> list();

    SysMenu getById(Long id);

    SysMenu create(SysMenu menu);

    SysMenu update(SysMenu menu);

    void delete(Long id);

    List<SysMenu> getMenuTree();

    List<SysMenu> getMenusByUsername(String username);
}
