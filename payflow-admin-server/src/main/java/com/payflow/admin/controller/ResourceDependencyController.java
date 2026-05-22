package com.payflow.admin.controller;

import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.service.guard.ResourceDeleteCheckResult;
import com.payflow.admin.service.guard.ResourceDeleteGuardService;
import com.payflow.admin.service.guard.ResourceType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 删除前资源依赖预检。
 */
@RestController
@RequestMapping("/api/v1/admin/resource-dependencies")
@RequiredArgsConstructor
public class ResourceDependencyController {

    private final ResourceDeleteGuardService resourceDeleteGuardService;

    /**
     * 查询删除前关联引用。
     *
     * @param resourceType 资源类型
     * @param resourceId   资源主键
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> checkDependencies(
            HttpServletRequest request,
            @RequestParam String resourceType,
            @RequestParam String resourceId) {
        if (!StringUtils.hasText(resourceType) || !StringUtils.hasText(resourceId)) {
            return ResponseEntity.ok(Map.of(
                    "code", 400,
                    "message", "resourceType 与 resourceId 不能为空",
                    "data", Map.of()));
        }
        ResourceType type;
        try {
            type = ResourceType.valueOf(resourceType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.ok(Map.of(
                    "code", 400,
                    "message", "不支持的 resourceType: " + resourceType,
                    "data", Map.of()));
        }
        Object parsedId = parseResourceId(type, resourceId.trim());
        List<String> scope = AdminRequestContext.merchantScope(request);
        ResourceDeleteCheckResult result = resourceDeleteGuardService.check(type, parsedId, scope);
        return ResponseEntity.ok(Map.of(
                "code", 0,
                "message", "success",
                "data", result));
    }

    private static Object parseResourceId(ResourceType type, String resourceId) {
        if (type == ResourceType.MERCHANT) {
            return resourceId;
        }
        try {
            return Long.parseLong(resourceId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("resourceId 必须为数字: " + resourceId);
        }
    }
}
