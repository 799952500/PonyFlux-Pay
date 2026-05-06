package com.payflow.admin.kit;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 商户支付路由「终端可见范围」解析与规范化（数据库存逗号分隔大写标记）。
 *
 * @author Lucas
 */
public final class ClientScopesKit {

    /** 默认三端均展示 */
    public static final String DEFAULT_DB_VALUE = "PC,H5,APP";

    private static final Set<String> ALLOWED = Set.of("PC", "H5", "APP");

    private ClientScopesKit() {
    }

    /**
     * 数据库字段 → 前端用列表；空或非法时退回三端全开。
     */
    public static List<String> parseToList(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of("PC", "H5", "APP");
        }
        List<String> parsed = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase())
                .filter(ALLOWED::contains)
                .distinct()
                .collect(Collectors.toList());
        if (parsed.isEmpty()) {
            return List.of("PC", "H5", "APP");
        }
        return parsed;
    }

    /**
     * 请求体中的列表或字符串 → 入库字段；无效标记丢弃后若为空则退回默认。
     */
    public static String normalizeToDb(Object clientScopes) {
        if (clientScopes == null) {
            return DEFAULT_DB_VALUE;
        }
        if (clientScopes instanceof List<?> list) {
            LinkedHashSet<String> set = new LinkedHashSet<>();
            for (Object o : list) {
                if (o == null) {
                    continue;
                }
                String s = o.toString().trim().toUpperCase();
                if (ALLOWED.contains(s)) {
                    set.add(s);
                }
            }
            if (set.isEmpty()) {
                return DEFAULT_DB_VALUE;
            }
            return String.join(",", set);
        }
        if (clientScopes instanceof String str) {
            return normalizeToDb(Arrays.asList(str.split(",")));
        }
        return DEFAULT_DB_VALUE;
    }
}
