package com.payflow.admin.service;

import com.payflow.admin.entity.SystemConfig;
import java.util.List;
import java.util.Map;
/**
 * @author Lucas
 */

public interface SystemConfigService {

    /** 启动时全量加载到缓存 */
    void loadAllToCache();

    /** 按分类查列表（null 表示全部） */
    List<SystemConfig> listByCategory(String category);

    /** 按 key 查值（从缓存，Cache-Aside） */
    String getValue(String key);

    /** 按 key 查值并转换为指定类型 */
    Object getValueAsType(String key, String type);

    /** 新增配置 */
    void save(SystemConfig config);

    /** 更新配置 */
    void update(Long id, SystemConfig config);

    /** 删除配置 */
    void delete(Long id);

    /** 刷新缓存——按 key */
    void refreshCacheByKey(String key);

    /** 刷新缓存——按分类 */
    void refreshCacheByCategory(String category);

    /** 刷新缓存——全部 */
    void refreshAllCache();

    /** 获取所有分类 */
    List<String> getAllCategories();

    /** 获取所有配置（key -> value map，用于快速读取） */
    Map<String, String> getAllConfigsMap();
}
