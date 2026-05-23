package com.payflow.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.payflow.admin.dto.SysUserSaveRequest;
import com.payflow.admin.dto.SysUserVO;
import com.payflow.admin.entity.SysUser;
import java.util.List;
/**
 * @author Lucas
 */

public interface SysUserService extends IService<SysUser> {
    List<SysUser> listUsers();

    List<SysUserVO> listUserVos();

    SysUser getById(Long id);

    SysUserVO getUserVo(Long id);

    void create(SysUser user);

    void createUser(SysUserSaveRequest request);

    void update(Long id, SysUser user);

    void updateUser(Long id, SysUserSaveRequest request);

    void resetPassword(Long id, String newPassword);

    void disable(Long id);
}
