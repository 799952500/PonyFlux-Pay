package com.payflow.admin.kit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.SystemConfig;
import com.payflow.admin.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置工具类
 *
 * 业务代码获取配置只需一行：
 *   String val  = SystemConfigKit.get("max_refund_rate");
 *   int    val  = SystemConfigKit.getInt("max_retry_times");
 *   double val  = SystemConfigKit.getDouble("min_payment_amount");
 *   boolean val = SystemConfigKit.getBool("enable_whitelist");
 *   long    val = SystemConfigKit.getLong("daily_limit_per_user");
 *
 * 特点：
 *   1. 静态方法调用，业务代码无需注入 Service
 *   2. 先读本地缓存（JVM ConcurrentHashMap），O(1) 命中
 *   3. 未命中则从 Redis 读（跨进程共享，可选）
 *   4. Redis 也没有则查数据库（Cache-Aside）
 *   5. 配置变更后调用 SystemConfigKit.refresh() 热更新本地缓存
 */
@Slf4j
@Component
@DependsOn("systemConfigServiceImpl")
public class SystemConfigKit {

    private static SystemConfigMapper systemConfigMapper;
    private static RedisTemplate<String, String> redisTemplate;

    /** 本地缓存（JVM 内存，进程重启后自动从 DB 重建） */
    private static final Map<String, String> LOCAL_CACHE = new ConcurrentHashMap<>();

    private static final String CACHE_PREFIX = "sys:config:";
    private static final String CACHE_KEY_SET = "sys:config:keys";

    @Autowired
    public void setSystemConfigMapper(SystemConfigMapper mapper) {
        SystemConfigKit.systemConfigMapper = mapper;
    }

    @Autowired(required = false)
    public void setRedisTemplate(RedisTemplate<String, String> redisTemplate) {
        SystemConfigKit.redisTemplate = redisTemplate;
    }

    /**
     * 启动时从数据库全量加载所有配置到本地缓存
     * （SystemConfigServiceImpl 的 @PostConstruct 之后执行）
     */
    @PostConstruct
    public void init() {
        refresh();
    }

    // ========== 核心方法 ==========

    /**
     * 获取配置值（字符串）
     * @param key 配置 key
     * @return 配置值，查不到返回 null
     */
    public static String get(String key) {
        if (key == null) return null;

        // 1. 本地缓存命中
        String value = LOCAL_CACHE.get(key);
        if (value != null) {
            return value;
        }

        // 2. Redis 命中（且更新本地缓存）
        if (isRedisAvailable()) {
            try {
                String redisValue = redisTemplate.opsForValue().get(CACHE_PREFIX + key);
                if (redisValue != null) {
                    LOCAL_CACHE.put(key, redisValue);
                    return redisValue;
                }
            } catch (Exception e) {
                log.warn("[SystemConfigKit] Redis 读取失败: {}", e.getMessage());
            }
        }

        // 3. Cache-Aside：查数据库
        String dbValue = loadFromDb(key);
        if (dbValue != null) {
            LOCAL_CACHE.put(key, dbValue);
            // 同步写入 Redis（可选，跨进程共享）
            if (isRedisAvailable()) {
                try {
                    redisTemplate.opsForValue().set(CACHE_PREFIX + key, dbValue);
                    redisTemplate.opsForSet().add(CACHE_KEY_SET, key);
                } catch (Exception e) {
                    log.warn("[SystemConfigKit] Redis 写入失败: {}", e.getMessage());
                }
            }
        }

        return dbValue;
    }

    /**
     * 获取配置值，带默认值（查不到时返回 defaultVal）
     */
    public static String get(String key, String defaultVal) {
        String val = get(key);
        return val != null ? val : defaultVal;
    }

    /**
     * 获取整型配置值
     * @param key 配置 key
     * @param defaultVal 查不到时返回的默认值
     */
    public static int getInt(String key, int defaultVal) {
        String val = get(key);
        if (val == null) return defaultVal;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("[SystemConfigKit] 配置值非整数 key={}, value={}", key, val);
            return defaultVal;
        }
    }

    /**
     * 获取整型配置值（查不到返回 0）
     */
    public static int getInt(String key) {
        return getInt(key, 0);
    }

    /**
     * 获取双精度配置值
     */
    public static double getDouble(String key, double defaultVal) {
        String val = get(key);
        if (val == null) return defaultVal;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            log.warn("[SystemConfigKit] 配置值非数字 key={}, value={}", key, val);
            return defaultVal;
        }
    }

    /**
     * 获取双精度配置值（查不到返回 0.0）
     */
    public static double getDouble(String key) {
        return getDouble(key, 0.0);
    }

    /**
     * 获取布尔配置值
     * 字符串 "true"/"1"/"yes"（不区分大小写）均视为 true
     */
    public static boolean getBool(String key, boolean defaultVal) {
        String val = get(key);
        if (val == null) return defaultVal;
        String v = val.trim().toLowerCase();
        return "true".equals(v) || "1".equals(v) || "yes".equals(v);
    }

    /**
     * 获取布尔配置值（查不到返回 false）
     */
    public static boolean getBool(String key) {
        return getBool(key, false);
    }

    /**
     * 获取 Long 配置值
     */
    public static long getLong(String key, long defaultVal) {
        String val = get(key);
        if (val == null) return defaultVal;
        try {
            return Long.parseLong(val.trim());
        } catch (NumberFormatException e) {
            log.warn("[SystemConfigKit] 配置值非 Long key={}, value={}", key, val);
            return defaultVal;
        }
    }

    /**
     * 获取 Long 配置值（查不到返回 0L）
     */
    public static long getLong(String key) {
        return getLong(key, 0L);
    }

    /**
     * 获取配置值（严格类型转换）
     * @param key 配置 key
     * @param type期望类型：NUMBER→Double, BOOLEAN→Boolean, INTEGER→Long, STRING→String
     */
    public static Object getTyped(String key, String type) {
        return switch (type.toUpperCase()) {
            case "NUMBER", "DOUBLE" -> getDouble(key);
            case "INTEGER", "INT" -> getLong(key);
            case "BOOLEAN" -> getBool(key);
            default -> get(key);
        };
    }

    /**
     * 检查某个配置是否存在（status=1）
     */
    public static boolean exists(String key) {
        return get(key) != null;
    }

    // ========== 管理方法 ==========

    /**
     * 全量刷新本地缓存（从数据库重新加载）
     * 配置变更后调用此方法热更新
     */
    public static void refresh() {
        log.info("[SystemConfigKit] 开始刷新本地缓存...");
        LOCAL_CACHE.clear();

        if (systemConfigMapper == null) {
            log.warn("[SystemConfigKit] SystemConfigMapper 未注入，跳过刷新");
            return;
        }

        try {
            var list = systemConfigMapper.selectList(
                    new LambdaQueryWrapper<SystemConfig>()
                            .eq(SystemConfig::getStatus, 1)
            );
            int count = 0;
            for (var cfg : list) {
                LOCAL_CACHE.put(cfg.getConfigKey(), cfg.getConfigValue());
                count++;
            }
            log.info("[SystemConfigKit] 本地缓存刷新完成，共 {} 条", count);
        } catch (Exception e) {
            log.error("[SystemConfigKit] 刷新本地缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 刷新单个 key（先删缓存再重新加载）
     */
    public static void refreshKey(String key) {
        LOCAL_CACHE.remove(key);
        String val = loadFromDb(key);
        if (val != null) {
            LOCAL_CACHE.put(key, val);
            // 同步写 Redis
            if (isRedisAvailable()) {
                try {
                    redisTemplate.opsForValue().set(CACHE_PREFIX + key, val);
                } catch (Exception ignored) {}
            }
        } else {
            // 配置被删除，同步清 Redis
            if (isRedisAvailable()) {
                try {
                    redisTemplate.delete(CACHE_PREFIX + key);
                } catch (Exception ignored) {}
            }
        }
        log.info("[SystemConfigKit] 缓存已刷新 key={}", key);
    }

    /**
     * 获取所有配置（Map 形式）
     */
    public static Map<String, String> getAll() {
        return Map.copyOf(LOCAL_CACHE);
    }

    // ========== 私有方法 ==========

    private static String loadFromDb(String key) {
        if (systemConfigMapper == null) return null;
        try {
            var cfg = systemConfigMapper.selectOne(
                    new LambdaQueryWrapper<SystemConfig>()
                            .eq(SystemConfig::getConfigKey, key)
                            .eq(SystemConfig::getStatus, 1)
            );
            return cfg != null ? cfg.getConfigValue() : null;
        } catch (Exception e) {
            log.error("[SystemConfigKit] 数据库读取失败 key={}: {}", key, e.getMessage());
            return null;
        }
    }

    private static boolean isRedisAvailable() {
        if (redisTemplate == null) return false;
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
