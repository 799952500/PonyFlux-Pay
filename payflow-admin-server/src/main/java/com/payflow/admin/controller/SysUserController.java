package com.payflow.admin.controller;

import com.payflow.admin.dto.SysUserSaveRequest;
import com.payflow.admin.dto.SysUserVO;
import com.payflow.admin.security.RequireRole;
import com.payflow.admin.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统用户管理 Controller
  * @author Lucas
 */
@Tag(name = "系统用户")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    @Operation(summary = "查询用户列表")
    @GetMapping
    public Map<String, Object> list() {
        List<SysUserVO> users = sysUserService.listUserVos();
        return Map.of("code", 0, "data", users, "message", "success");
    }

    @Operation(summary = "查询用户详情")
    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        SysUserVO user = sysUserService.getUserVo(id);
        if (user == null) {
            return Map.of("code", 404, "message", "用户不存在");
        }
        return Map.of("code", 0, "data", user, "message", "success");
    }

    @Operation(summary = "新增系统用户")
    @RequireRole(RequireRole.SUPER_ADMIN)
    @PostMapping
    public Map<String, Object> create(@RequestBody SysUserSaveRequest request) {
        sysUserService.createUser(request);
        return Map.of("code", 0, "message", "用户创建成功");
    }

    @Operation(summary = "更新系统用户")
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody SysUserSaveRequest request) {
        sysUserService.updateUser(id, request);
        return Map.of("code", 0, "message", "用户更新成功");
    }

    @Operation(summary = "重置用户密码")
    @RequireRole(RequireRole.SUPER_ADMIN)
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

    @Operation(summary = "禁用用户")
    @PutMapping("/{id}/disable")
    public Map<String, Object> disable(@PathVariable Long id) {
        sysUserService.disable(id);
        return Map.of("code", 0, "message", "用户已禁用");
    }
}
