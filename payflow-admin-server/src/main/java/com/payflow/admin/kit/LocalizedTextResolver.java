package com.payflow.admin.kit;

import com.payflow.admin.entity.PaymentMethod;
import org.springframework.util.StringUtils;

/**
 * 按展示语言从支付方式多语言列解析文案。
 */
public final class LocalizedTextResolver {

    private static final String DEFAULT_LOCALE = "zh-CN";

    private LocalizedTextResolver() {
    }

    public static String normalizeLocale(String locale) {
        if (!StringUtils.hasText(locale)) {
            return DEFAULT_LOCALE;
        }
        String trimmed = locale.trim();
        if ("zh-TW".equals(trimmed) || "en-US".equals(trimmed) || "zh-CN".equals(trimmed)) {
            return trimmed;
        }
        String lower = trimmed.toLowerCase();
        if ("zh-tw".equals(lower) || "zh_hant".equals(lower)) {
            return "zh-TW";
        }
        if (lower.startsWith("en")) {
            return "en-US";
        }
        return DEFAULT_LOCALE;
    }

    public static String resolveMethodName(PaymentMethod pm, String locale) {
        if (pm == null) {
            return "";
        }
        return switch (normalizeLocale(locale)) {
            case "zh-TW" -> firstNonBlank(pm.getMethodNameZhTw(), pm.getMethodNameZhCn(), pm.getMethodName());
            case "en-US" -> firstNonBlank(pm.getMethodNameEn(), pm.getMethodNameZhCn(), pm.getMethodName());
            default -> firstNonBlank(pm.getMethodNameZhCn(), pm.getMethodName());
        };
    }

    public static String resolveDescription(PaymentMethod pm, String locale) {
        if (pm == null) {
            return "";
        }
        return switch (normalizeLocale(locale)) {
            case "zh-TW" -> firstNonBlank(pm.getDescriptionZhTw(), pm.getDescriptionZhCn(), pm.getDescription());
            case "en-US" -> firstNonBlank(pm.getDescriptionEn(), pm.getDescriptionZhCn(), pm.getDescription());
            default -> firstNonBlank(pm.getDescriptionZhCn(), pm.getDescription());
        };
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return "";
        }
        for (String c : candidates) {
            if (StringUtils.hasText(c)) {
                return c.trim();
            }
        }
        return "";
    }
}
