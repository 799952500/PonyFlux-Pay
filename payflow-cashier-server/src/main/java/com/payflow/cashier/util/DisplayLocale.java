package com.payflow.cashier.util;

import org.springframework.util.StringUtils;

import java.util.Set;

/**
 * 收银台受支持的展示语言标识。
 */
public final class DisplayLocale {

    public static final String DEFAULT = "zh-CN";

    public static final Set<String> SUPPORTED = Set.of("zh-CN", "zh-TW", "en-US");

    private DisplayLocale() {
    }

    /**
     * 规范化语言标识；缺失或非法时回退 {@link #DEFAULT}。
     */
    public static String normalize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return DEFAULT;
        }
        String trimmed = raw.trim();
        if (SUPPORTED.contains(trimmed)) {
            return trimmed;
        }
        String lower = trimmed.toLowerCase();
        if ("zh-tw".equals(lower) || "zh_hant".equals(lower)) {
            return "zh-TW";
        }
        if ("zh-cn".equals(lower) || "zh".equals(lower)) {
            return "zh-CN";
        }
        if (lower.startsWith("en")) {
            return "en-US";
        }
        return DEFAULT;
    }
}
