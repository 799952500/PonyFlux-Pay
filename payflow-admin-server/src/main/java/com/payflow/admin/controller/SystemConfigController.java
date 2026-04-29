package com.payflow.admin.controller;

import com.payflow.admin.entity.SystemConfig;
import com.payflow.admin.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统配置管理 Controller
  * @author Lucas
 */
@RestController
@RequestMapping("/api/v1/admin/system-configs")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    /**
     * 查询配置列表（支持按分类筛选）
     *
     * @param category 配置分类（可选，如 risk/payment/fee）
     * @return 配置列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@RequestParam(required = false) String category) {
        List<SystemConfig> list = systemConfigService.listByCategory(category);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", list));
    }

    /**
     * 查询所有配置分类
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> categories() {
        List<String> cats = systemConfigService.getAllCategories();
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", cats));
    }

    /**
     * 根据 key 查询单个配置值
     *
     * @param key 配置键
     * @return 配置值
     */
    @GetMapping("/{key}")
    public ResponseEntity<Map<String, Object>> getByKey(@PathVariable String key) {
        String value = systemConfigService.getValue(key);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", value != null ? value : ""));
    }

    /**
     * 以 Map 形式返回所有配置（key-value 结构）
     *
     * @return 全部配置的键值对
     */
    @GetMapping("/map")
    public ResponseEntity<Map<String, Object>> getAllMap() {
        Map<String, String> map = systemConfigService.getAllConfigsMap();
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", map));
    }

    /**
     * 新增系统配置
     *
     * @param config 配置信息
     * @return 操作结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody SystemConfig config) {
        systemConfigService.save(config);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    /**
     * 更新系统配置
     *
     * @param id     配置ID
     * @param config 配置信息
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> update(@PathVariable Long id,
                                                        @RequestBody SystemConfig config) {
        config.setId(id);
        systemConfigService.update(id, config);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    /**
     * 删除系统配置
     *
     * @param id 配置ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        systemConfigService.delete(id);
        return ResponseEntity.ok(Map.of("code", 0, "message", "success", "data", Map.of()));
    }

    /**
     * 刷新指定 key 的缓存
     *
     * @param key 配置键
     * @return 操作结果
     */
    @PostMapping("/cache/refresh/{key}")
    public ResponseEntity<Map<String, Object>> refreshByKey(@PathVariable String key) {
        systemConfigService.refreshCacheByKey(key);
        return ResponseEntity.ok(Map.of("code", 0, "message", "缓存已刷新（key）", "data", Map.of()));
    }

    /**
     * 刷新指定分类的缓存
     *
     * @param category 配置分类
     * @return 操作结果
     */
    @PostMapping("/cache/refresh/category/{category}")
    public ResponseEntity<Map<String, Object>> refreshByCategory(@PathVariable String category) {
        systemConfigService.refreshCacheByCategory(category);
        return ResponseEntity.ok(Map.of("code", 0, "message", "缓存已刷新（分类）", "data", Map.of()));
    }

    /**
     * 全量刷新所有配置缓存
     *
     * @return 操作结果
     */
    @PostMapping("/cache/refresh/all")
    public ResponseEntity<Map<String, Object>> refreshAll() {
        systemConfigService.refreshAllCache();
        return ResponseEntity.ok(Map.of("code", 0, "message", "全量缓存已刷新", "data", Map.of()));
    }
}
