package com.payflow.admin.controller;

import com.payflow.admin.entity.SysMenu;
import com.payflow.admin.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 菜单权限管理 Controller
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService sysMenuService;

    /**
     * 获取完整菜单树（用于侧边栏渲染）
     *
     * @return 树形结构的完整菜单列表
     */
    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> menuTree() {
        List<SysMenu> tree = sysMenuService.getMenuTree();
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", tree));
    }

    /**
     * 根据ID查询菜单详情
     *
     * @param id 菜单ID
     * @return 菜单详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long id) {
        SysMenu menu = sysMenuService.getById(id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", menu));
    }

    /**
     * 新增菜单
     *
     * @param menu 菜单信息
     * @return 新增的菜单
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody SysMenu menu) {
        SysMenu created = sysMenuService.create(menu);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", created));
    }

    /**
     * 更新菜单信息
     *
     * @param id   菜单ID
     * @param menu 菜单信息
     * @return 更新后的菜单
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                        @RequestBody SysMenu menu) {
        menu.setId(id);
        SysMenu updated = sysMenuService.update(menu);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", updated));
    }

    /**
     * 删除菜单
     *
     * @param id 菜单ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        sysMenuService.delete(id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", (Object) null));
    }
}
