package com.payflow.admin.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 按钮权限 enforcement 配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "payflow.permission")
public class PermissionProperties {

    /**
     * 是否启用 @RequirePermission 拦截；false 时仅记录日志不拒绝。
     */
    private boolean enforceButton = true;

    /**
     * Redis 缓存 TTL（秒）。
     */
    private long cacheTtlSeconds = 300;
}
