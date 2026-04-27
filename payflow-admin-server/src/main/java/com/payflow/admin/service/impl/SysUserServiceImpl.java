package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.payflow.admin.entity.SysUser;
import com.payflow.admin.mapper.SysUserMapper;
import com.payflow.admin.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public List<SysUser> listUsers() {
        List<SysUser> users = list();
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public SysUser getById(Long id) {
        SysUser user = baseMapper.selectById(id);
        if (user != null) user.setPassword(null);
        return user;
    }

    @Override
    public void create(SysUser user) {
        // BCrypt 加密密码
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        save(user);
    }

    @Override
    public void update(Long id, SysUser user) {
        SysUser existing = getById(id);
        if (existing == null) {
            throw new RuntimeException("用户不存在");
        }
        // 若有新密码则加密
        if (user.getPassword() != null && !user.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null); // 不更新密码字段
        }
        user.setId(id);
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        SysUser user = getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }

    @Override
    public void disable(Long id) {
        SysUser user = getById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setStatus("DISABLED");
        user.setUpdatedAt(LocalDateTime.now());
        updateById(user);
    }
}
