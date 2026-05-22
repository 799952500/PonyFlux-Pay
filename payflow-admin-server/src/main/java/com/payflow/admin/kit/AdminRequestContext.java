package com.payflow.admin.kit;

import com.payflow.common.exception.BizException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
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

    /**
     * 将请求商户筛选与授权范围求交；返回 {@code __NO_ACCESS__} 表示无权限。
     */
    public static String resolveMerchantFilter(String merchantId, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return StringUtils.hasText(merchantId) ? merchantId.trim() : null;
        }
        if (merchantScopeIds.isEmpty()) {
            return "__NO_ACCESS__";
        }
        if (StringUtils.hasText(merchantId)) {
            String requested = merchantId.trim();
            return merchantScopeIds.contains(requested) ? requested : "__NO_ACCESS__";
        }
        return merchantScopeIds.size() == 1 ? merchantScopeIds.get(0) : null;
    }

    public static boolean isMerchantAllowed(String merchantId, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return true;
        }
        return StringUtils.hasText(merchantId) && merchantScopeIds.contains(merchantId.trim());
    }

    public static void assertMerchantAllowed(String merchantId, List<String> merchantScopeIds) {
        if (!isMerchantAllowed(merchantId, merchantScopeIds)) {
            throw new BizException(6101, "无权访问该资源");
        }
    }

    public static void assertAnyMerchantAllowed(Collection<String> merchantIds, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return;
        }
        if (merchantIds == null || merchantIds.isEmpty()) {
            throw new BizException(6101, "无权访问该资源");
        }
        for (String merchantId : merchantIds) {
            if (!isMerchantAllowed(merchantId, merchantScopeIds)) {
                throw new BizException(6101, "无权访问该资源");
            }
        }
    }

    /**
     * 写入类接口解析目标商户号：平台管理员可使用请求体中的商户号；商户管理员强制为授权范围（单商户时自动回填）。
     */
    public static String resolveMerchantIdForWrite(HttpServletRequest request, String requestedMerchantId) {
        return resolveMerchantIdForWrite(merchantScope(request), requestedMerchantId);
    }

    public static String resolveMerchantIdForWrite(List<String> merchantScopeIds, String requestedMerchantId) {
        if (merchantScopeIds == null) {
            return StringUtils.hasText(requestedMerchantId) ? requestedMerchantId.trim() : null;
        }
        if (merchantScopeIds.isEmpty()) {
            throw new BizException(6101, "无权访问该资源");
        }
        if (merchantScopeIds.size() == 1) {
            String authorized = merchantScopeIds.get(0);
            if (StringUtils.hasText(requestedMerchantId) && !authorized.equals(requestedMerchantId.trim())) {
                throw new BizException(6101, "无权为其他商户创建数据");
            }
            return authorized;
        }
        if (!StringUtils.hasText(requestedMerchantId)) {
            throw new BizException(6101, "请指定商户号");
        }
        assertMerchantAllowed(requestedMerchantId, merchantScopeIds);
        return requestedMerchantId.trim();
    }
}
