package com.payflow.admin.service;

import com.payflow.admin.entity.SysRole;

import java.util.List;
/**
 * @author Lucas
 */

public interface SysUserRoleService {

    List<SysRole> getRolesByUserId(Long userId);

    void assignRoles(Long userId, List<Long> roleIds);
}
