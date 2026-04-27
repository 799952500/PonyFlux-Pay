package com.payflow.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payflow.admin.entity.SysUser;
import java.util.List;

public interface SysUserService extends IService<SysUser> {
    List<SysUser> listUsers();
    SysUser getById(Long id);
    void create(SysUser user);
    void update(Long id, SysUser user);
    void resetPassword(Long id, String newPassword);
    void disable(Long id);
}
