package com.payflow.admin.kit;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 从 JWT 拦截器写入的请求属性解析数据权限范围。
 *
 * @author Lucas
 */
public final class AdminRequestContext {

    private AdminRequestContext() {
    }

    /**
     * @return null 表示不做商户范围限制（含 SUPER_ADMIN）；非空列表表示仅能访问所列商户号数据
     */
    public static List<String> merchantScope(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object roleAttr = request.getAttribute("role");
        String role = roleAttr != null ? roleAttr.toString() : "";
        if ("SUPER_ADMIN".equals(role)) {
            return null;
        }
        Object raw = request.getAttribute("dataMerchantIds");
        if (raw == null || raw.toString().isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(raw.toString().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
