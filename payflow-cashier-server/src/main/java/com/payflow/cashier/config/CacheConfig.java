package com.payflow.cashier.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Spring Cache 配置类
 * 用于启用 Spring Cache 注解缓存
 * 仅当 Redis 连接可用时启用（payflow.cache.redis.enabled=true）
 *
 * @author PayFlow Team
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "payflow.cache.redis.enabled", havingValue = "true", matchIfMissing = false)
public class CacheConfig {

    /** 订单缓存名称 */
    public static final String CACHE_NAME_ORDER = "orderCache";

    /** 缓存 TTL（分钟） */
    private static final long CACHE_TTL_MINUTES = 30;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 配置默认缓存序列化
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                // 使用 String 序列化 key
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // 使用 JSON 序列化 value
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 设置 TTL
                .entryTtl(Duration.ofMinutes(CACHE_TTL_MINUTES))
                // 禁用空值缓存
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .transactionAware()
                .build();
    }
}