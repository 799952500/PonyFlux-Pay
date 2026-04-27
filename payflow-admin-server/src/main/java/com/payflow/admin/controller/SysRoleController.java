package com.payflow.admin.controller;

import com.payflow.admin.entity.SysMenu;
import com.payflow.admin.entity.SysRole;
import com.payflow.admin.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色权限管理 Controller
 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService sysRoleService;

    /**
     * 查询所有角色列表
     *
     * @return 角色列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list() {
        List<SysRole> roles = sysRoleService.list();
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", roles));
    }

    /**
     * 根据ID查询角色详情
     *
     * @param id 角色ID
     * @return 角色详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        SysRole role = sysRoleService.getById(id);
        if (role == null) {
            return ResponseEntity.ok(Map.of("code", 1, "message", "角色不存在", "data", Map.of()));
        }
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", role));
    }

    /**
     * 新增角色
     *
     * @param role 角色信息
     * @return 新增的角色
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody SysRole role) {
        SysRole created = sysRoleService.create(role);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", created));
    }

    /**
     * 更新角色信息
     *
     * @param id   角色ID
     * @param role 角色信息
     * @return 更新后的角色
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                       @RequestBody SysRole role) {
        role.setId(id);
        SysRole updated = sysRoleService.update(role);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", updated));
    }

    /**
     * 删除角色
     *
     * @param id 角色ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        try {
            sysRoleService.delete(id);
            return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(Map.of("code", 1, "message", e.getMessage(), "data", Map.of()));
        }
    }

    /**
     * 查询指定角色的菜单权限列表
     *
     * @param id 角色ID
     * @return 该角色关联的菜单列表
     */
    @GetMapping("/{id}/menus")
    public ResponseEntity<Map<String, Object>> getRoleMenus(@PathVariable Long id) {
        List<SysMenu> menus = sysRoleService.getMenusByRoleId(id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", menus));
    }

    /**
     * 为角色分配菜单权限
     *
     * @param id   角色ID
     * @param body 包含 menuIds 列表
     * @return 操作结果
     */
    @PostMapping("/{id}/menus")
    public ResponseEntity<Map<String, Object>> assignMenus(@PathVariable Long id,
                                                            @RequestBody Map<String, List<Long>> body) {
        List<Long> menuIds = body.get("menuIds");
        sysRoleService.assignMenus(id, menuIds);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }
}
