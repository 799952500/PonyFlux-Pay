package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payflow.admin.dto.SysUserSaveRequest;
import com.payflow.admin.dto.SysUserVO;
import com.payflow.admin.entity.SysRole;
import com.payflow.admin.entity.SysUser;
import com.payflow.admin.mapper.SysUserMapper;
import com.payflow.admin.service.SysUserRoleService;
import com.payflow.admin.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
/**
 * @author Lucas
 */
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SysUserRoleService sysUserRoleService;

    @Override
    public List<SysUser> listUsers() {
        List<SysUser> users = list();
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public List<SysUserVO> listUserVos() {
        return listUsers().stream()
                .map(user -> toVo(user, resolvePrimaryRoleId(user.getId())))
                .collect(Collectors.toList());
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = baseMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public SysUserVO getUserVo(Long id) {
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            return null;
        }
        user.setPassword(null);
        return toVo(user, resolvePrimaryRoleId(id));
    }

    @Override
    public void create(SysUser user) {
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new IllegalArgumentException("初始密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword().trim()));
        if (user.getStatus() == null || user.getStatus().isBlank()) {
            user.setStatus("ACTIVE");
        }
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        save(user);
    }

    @Override
    @Transactional
    public void createUser(SysUserSaveRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (request.getRoleId() == null) {
            throw new IllegalArgumentException("请选择角色");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername().trim());
        user.setPassword(request.getPassword());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setStatus(request.getStatus());
        create(user);
        sysUserRoleService.assignRoles(user.getId(), List.of(request.getRoleId()));
    }

    @Override
    public void update(Long id, SysUser user) {
        SysUser existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        user.setId(id);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    @Override
    @Transactional
    public void updateUser(Long id, SysUserSaveRequest request) {
        SysUser existing = baseMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        SysUser patch = new SysUser();
        patch.setId(id);
        patch.setNickname(request.getNickname());
        patch.setPhone(request.getPhone());
        patch.setEmail(request.getEmail());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            patch.setStatus(request.getStatus());
        }
        patch.setUpdatedAt(LocalDateTime.now());
        updateById(patch);
        if (request.getRoleId() != null) {
            sysUserRoleService.assignRoles(id, List.of(request.getRoleId()));
        }
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    @Override
    public void disable(Long id) {
        SysUser user = baseMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus("DISABLED");
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    private Long resolvePrimaryRoleId(Long userId) {
        List<SysRole> roles = sysUserRoleService.getRolesByUserId(userId);
        if (roles.isEmpty()) {
            return null;
        }
        return roles.get(0).getId();
    }

    private SysUserVO toVo(SysUser user, Long roleId) {
        SysUserVO vo = new SysUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setStatus(user.getStatus());
        vo.setRoleId(roleId);
        vo.setCreatedAt(user.getCreatedAt());
        vo.setUpdatedAt(user.getUpdatedAt());
        return vo;
    }
}
