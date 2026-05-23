package com.payflow.admin.kit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 商户回调请求参数展示脱敏。
 */
public final class MerchantNotifyMaskKit {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private MerchantNotifyMaskKit() {
    }

    public static Object maskRequestParams(String requestParamsJson) {
        if (requestParamsJson == null || requestParamsJson.isBlank()) {
            return Map.of();
        }
        try {
            Map<String, Object> map = OBJECT_MAPPER.readValue(
                    requestParamsJson, new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            Map<String, Object> masked = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (shouldMaskKey(key) && value != null) {
                    masked.put(key, maskValue(String.valueOf(value)));
                } else {
                    masked.put(key, value);
                }
            }
            return masked;
        } catch (Exception ex) {
            return requestParamsJson;
        }
    }

    private static boolean shouldMaskKey(String key) {
        if (key == null) {
            return false;
        }
        String lower = key.toLowerCase(Locale.ROOT);
        return "sign".equals(lower)
                || lower.contains("secret")
                || lower.contains("password")
                || lower.endsWith("key");
    }

    public static String maskValue(String value) {
        if (value == null || value.isBlank()) {
            return "****";
        }
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
