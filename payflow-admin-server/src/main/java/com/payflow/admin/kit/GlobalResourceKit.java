package com.payflow.admin.kit;

import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 全局配置与公共资源访问辅助。
 */
public final class GlobalResourceKit {

    public static final String CLASSIFICATION_GLOBAL = "GLOBAL";
    public static final String CLASSIFICATION_MERCHANT = "MERCHANT";

    private GlobalResourceKit() {
    }

    public static boolean isSensitiveConfigKey(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            return false;
        }
        String lower = configKey.toLowerCase(Locale.ROOT);
        return lower.contains("secret")
                || lower.contains("password")
                || lower.contains("token")
                || lower.contains("key")
                || lower.contains("credential");
    }

    public static String maskConfigValue(String configKey, String value, boolean platformAdmin) {
        if (platformAdmin || !isSensitiveConfigKey(configKey)) {
            return value;
        }
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return "******";
    }

    public static Map<String, Object> globalResourceMeta() {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("classification", CLASSIFICATION_GLOBAL);
        meta.put("merchantScoped", false);
        return meta;
    }
}
