package com.payflow.admin.controller;

import com.payflow.admin.entity.SysUser;
import com.payflow.admin.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统用户管理 Controller
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    /**
     * 查询所有用户列表
     *
     * @return 用户列表（密码字段已清除）
     */
    @GetMapping
    public Map<String, Object> list() {
        List<SysUser> users = sysUserService.listUsers();
        return Map.of("code", 0, "data", users, "message", "success");
    }

    /**
     * 根据ID查询用户详情
     *
     * @param id 用户ID
     * @return 用户详情（密码字段已清除）
     */
    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        SysUser user = sysUserService.getById(id);
        if (user == null) {
            return Map.of("code", 404, "message", "用户不存在");
        }
        // 防止密码泄漏
        user.setPassword(null);
        return Map.of("code", 0, "data", user, "message", "success");
    }

    /**
     * 新增系统用户
     *
     * @param user 用户信息（包含明文密码，将被 BCrypt 加密存储）
     * @return 操作结果
     */
    @PostMapping
    public Map<String, Object> create(@RequestBody SysUser user) {
        sysUserService.create(user);
        return Map.of("code", 0, "message", "用户创建成功");
    }

    /**
     * 更新系统用户信息
     *
     * @param id   用户ID
     * @param user 用户信息
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody SysUser user) {
        sysUserService.update(id, user);
        return Map.of("code", 0, "message", "用户更新成功");
    }

    /**
     * 重置用户密码
     *
     * @param id   用户ID
     * @param body 包含新密码
     * @return 操作结果
     */
    @PutMapping("/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id,
                                             @RequestBody Map<String, String> body) {
        String newPwd = body.get("newPassword");
        if (newPwd == null || newPwd.isBlank()) {
            return Map.of("code", 400, "message", "新密码不能为空");
        }
        sysUserService.resetPassword(id, newPwd);
        return Map.of("code", 0, "message", "密码已重置");
    }

    /**
     * 禁用指定用户
     *
     * @param id 用户ID
     * @return 操作结果
     */
    @PutMapping("/{id}/disable")
    public Map<String, Object> disable(@PathVariable Long id) {
        sysUserService.disable(id);
        return Map.of("code", 0, "message", "用户已禁用");
    }
}
