package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.SystemConfig;
import com.payflow.admin.mapper.SystemConfigMapper;
import com.payflow.admin.service.SystemConfigService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final RedisTemplate<String, String> redisTemplate;

    /** Redis key 前缀（与订单缓存 order:* 隔离） */
    private static final String CACHE_PREFIX = "sys:config:";
    /** 记录所有已缓存 key 的 Set（用于 refreshAllCache） */
    private static final String CACHE_KEY_SET = "sys:config:keys";

    @Autowired(required = false)
    public SystemConfigServiceImpl(SystemConfigMapper systemConfigMapper, RedisTemplate<String, String> redisTemplate) {
        this.systemConfigMapper = systemConfigMapper;
        this.redisTemplate = redisTemplate;
    }

    /** 无 Redis 构造器（降级模式） */
    public SystemConfigServiceImpl(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
        this.redisTemplate = null;
        log.warn("[SystemConfig] Redis 未配置，系统配置缓存功能已禁用（降级模式）");
    }

    @PostConstruct
    public void init() {
        if (redisTemplate != null) {
            loadAllToCache();
        } else {
            log.info("[SystemConfig] 跳过缓存加载（降级模式），系统配置将直连数据库");
        }
    }

    @Override
    public void loadAllToCache() {
        if (!isRedisAvailable()) {
            log.warn("[SystemConfig] Redis 不可用，跳过缓存加载");
            return;
        }
        log.info("[SystemConfig] 开始加载系统配置到缓存...");
        try {
            List<SystemConfig> all = systemConfigMapper.selectList(null);
            int count = 0;
            for (SystemConfig cfg : all) {
                if (cfg.getStatus() != null && cfg.getStatus() == 1) {
                    redisTemplate.opsForValue().set(CACHE_PREFIX + cfg.getConfigKey(), cfg.getConfigValue());
                    redisTemplate.opsForSet().add(CACHE_KEY_SET, cfg.getConfigKey());
                    count++;
                }
            }
            log.info("[SystemConfig] 缓存加载完成，共 {} 条配置", count);
        } catch (Exception e) {
            log.error("[SystemConfig] 缓存加载失败，切换降级模式: {}", e.getMessage());
        }
    }

    @Override
    public String getValue(String key) {
        if (isRedisAvailable()) {
            try {
                String cacheKey = CACHE_PREFIX + key;
                String value = redisTemplate.opsForValue().get(cacheKey);
                if (value != null) {
                    return value;
                }
                // Cache-Aside：从 DB 加载
                LambdaQueryWrapper<SystemConfig> q = new LambdaQueryWrapper<SystemConfig>()
                        .eq(SystemConfig::getConfigKey, key)
                        .eq(SystemConfig::getStatus, 1);
                SystemConfig cfg = systemConfigMapper.selectOne(q);
                if (cfg != null) {
                    value = cfg.getConfigValue();
                    redisTemplate.opsForValue().set(cacheKey, value);
                    redisTemplate.opsForSet().add(CACHE_KEY_SET, key);
                }
                return value;
            } catch (Exception e) {
                log.error("[SystemConfig] Redis 读取失败，降级到数据库: {}", e.getMessage());
            }
        }
        // 降级：直连数据库
        LambdaQueryWrapper<SystemConfig> q = new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, key)
                .eq(SystemConfig::getStatus, 1);
        SystemConfig cfg = systemConfigMapper.selectOne(q);
        return cfg != null ? cfg.getConfigValue() : null;
    }

    @Override
    public Object getValueAsType(String key, String type) {
        String value = getValue(key);
        if (value == null) return null;
        return switch (type) {
            case "NUMBER" -> Double.parseDouble(value);
            case "BOOLEAN" -> Boolean.parseBoolean(value);
            case "JSON" -> value;
            default -> value;
        };
    }

    @Override
    public List<SystemConfig> listByCategory(String category) {
        LambdaQueryWrapper<SystemConfig> q = new LambdaQueryWrapper<SystemConfig>()
                .eq(category != null, SystemConfig::getCategory, category)
                .orderByAsc(SystemConfig::getSortOrder);
        return systemConfigMapper.selectList(q);
    }

    @Override
    @Transactional
    public void save(SystemConfig config) {
        systemConfigMapper.insert(config);
        if (isRedisAvailable()) {
            try {
                if (config.getStatus() != null && config.getStatus() == 1) {
                    redisTemplate.opsForValue().set(CACHE_PREFIX + config.getConfigKey(), config.getConfigValue());
                    redisTemplate.opsForSet().add(CACHE_KEY_SET, config.getConfigKey());
                }
            } catch (Exception e) {
                log.error("[SystemConfig] Redis 写入失败（save）: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void update(Long id, SystemConfig config) {
        SystemConfig existing = systemConfigMapper.selectById(id);
        if (existing != null && isRedisAvailable()) {
            try {
                redisTemplate.delete(CACHE_PREFIX + existing.getConfigKey());
                redisTemplate.opsForSet().remove(CACHE_KEY_SET, existing.getConfigKey());
            } catch (Exception e) {
                log.error("[SystemConfig] Redis 删除失败（update）: {}", e.getMessage());
            }
        }
        SystemConfig toUpdate = new SystemConfig();
        toUpdate.setId(id);
        toUpdate.setConfigKey(config.getConfigKey());
        toUpdate.setConfigValue(config.getConfigValue());
        toUpdate.setValueType(config.getValueType());
        toUpdate.setCategory(config.getCategory());
        toUpdate.setDescription(config.getDescription());
        toUpdate.setSortOrder(config.getSortOrder());
        toUpdate.setStatus(config.getStatus());
        systemConfigMapper.updateById(toUpdate);
        if (isRedisAvailable()) {
            try {
                if (config.getStatus() != null && config.getStatus() == 1) {
                    redisTemplate.opsForValue().set(CACHE_PREFIX + config.getConfigKey(), config.getConfigValue());
                    redisTemplate.opsForSet().add(CACHE_KEY_SET, config.getConfigKey());
                }
            } catch (Exception e) {
                log.error("[SystemConfig] Redis 写入失败（update）: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SystemConfig cfg = systemConfigMapper.selectById(id);
        if (cfg != null && isRedisAvailable()) {
            try {
                redisTemplate.delete(CACHE_PREFIX + cfg.getConfigKey());
                redisTemplate.opsForSet().remove(CACHE_KEY_SET, cfg.getConfigKey());
            } catch (Exception e) {
                log.error("[SystemConfig] Redis 删除失败（delete）: {}", e.getMessage());
            }
        }
        systemConfigMapper.deleteById(id);
    }

    @Override
    public void refreshCacheByKey(String key) {
        if (!isRedisAvailable()) {
            log.warn("[SystemConfig] Redis 未启动，无法刷新缓存（按 key）");
            return;
        }
        try {
            redisTemplate.delete(CACHE_PREFIX + key);
            log.info("[SystemConfig] 缓存已刷新（按 key）: {}", key);
        } catch (Exception e) {
            log.error("[SystemConfig] Redis 操作失败: {}", e.getMessage());
        }
    }

    @Override
    public void refreshCacheByCategory(String category) {
        if (!isRedisAvailable()) {
            log.warn("[SystemConfig] Redis 未启动，无法刷新缓存（按分类）");
            return;
        }
        try {
            LambdaQueryWrapper<SystemConfig> q = new LambdaQueryWrapper<SystemConfig>()
                    .eq(SystemConfig::getCategory, category);
            List<SystemConfig> configs = systemConfigMapper.selectList(q);
            for (SystemConfig cfg : configs) {
                redisTemplate.delete(CACHE_PREFIX + cfg.getConfigKey());
                redisTemplate.opsForSet().remove(CACHE_KEY_SET, cfg.getConfigKey());
            }
            log.info("[SystemConfig] 缓存已刷新（按分类）: {}，共 {} 条", category, configs.size());
        } catch (Exception e) {
            log.error("[SystemConfig] Redis 操作失败: {}", e.getMessage());
        }
    }

    @Override
    public void refreshAllCache() {
        if (!isRedisAvailable()) {
            log.warn("[SystemConfig] Redis 未启动，无法全量刷新缓存");
            return;
        }
        try {
            Set<String> keys = redisTemplate.opsForSet().members(CACHE_KEY_SET);
            if (keys != null) {
                for (String key : keys) {
                    redisTemplate.delete(CACHE_PREFIX + key);
                }
                redisTemplate.delete(CACHE_KEY_SET);
            }
            loadAllToCache();
            log.info("[SystemConfig] 全量缓存已刷新");
        } catch (Exception e) {
            log.error("[SystemConfig] Redis 操作失败: {}", e.getMessage());
        }
    }

    @Override
    public List<String> getAllCategories() {
        return systemConfigMapper.selectList(null).stream()
                .map(SystemConfig::getCategory)
                .filter(c -> c != null)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> getAllConfigsMap() {
        Map<String, String> map = new HashMap<>();
        List<SystemConfig> all = systemConfigMapper.selectList(
                new LambdaQueryWrapper<SystemConfig>().eq(SystemConfig::getStatus, 1)
        );
        for (SystemConfig cfg : all) {
            map.put(cfg.getConfigKey(), cfg.getConfigValue());
        }
        return map;
    }

    /** 检查 Redis 是否可用 */
    private boolean isRedisAvailable() {
        if (redisTemplate == null) return false;
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
