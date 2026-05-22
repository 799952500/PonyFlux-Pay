package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.AdminAuditLog;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.mapper.AdminAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志写入与查询。
 *
 * @author Lucas
 */
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AdminAuditLogMapper adminAuditLogMapper;

    /**
     * 记录一条审计日志。
     */
    public void record(String username, String action, String resourcePath, String detail, String clientIp) {
        record(username, null, null, action, resourcePath, null, null, detail, "SUCCESS", null, clientIp);
    }

    public void record(String username, String operatorType, String merchantId, String action,
                       String resourcePath, String resourceType, String resourceId, String detail,
                       String result, String denyReason, String clientIp) {
        AdminAuditLog row = new AdminAuditLog();
        row.setUsername(username != null ? username : "");
        row.setOperatorType(operatorType);
        row.setMerchantId(merchantId);
        row.setAction(action != null ? action : "");
        row.setResourcePath(resourcePath != null ? resourcePath : "");
        row.setResourceType(resourceType);
        row.setResourceId(resourceId);
        row.setDetail(truncate(detail, 1000));
        row.setResult(result != null ? result : "SUCCESS");
        row.setDenyReason(truncate(denyReason, 250));
        row.setClientIp(clientIp != null ? clientIp : "");
        row.setCreatedAt(LocalDateTime.now());
        adminAuditLogMapper.insert(row);
    }

    public void recordDenied(String username, String merchantId, String resourceType,
                             String resourceId, String resourcePath, String clientIp) {
        record(username, "MERCHANT_ADMIN", merchantId, "DENY", resourcePath, resourceType,
                resourceId, "跨商户访问被拒绝", "DENIED", "授权商户范围不包含目标资源", clientIp);
    }

    /**
     * 登录结果审计（不含密码）。
     */
    public void recordLogin(String username, boolean success, String clientIp) {
        record(username, "LOGIN", "/api/v1/admin/auth/login", success ? "登录成功" : "登录失败", clientIp);
    }

    /**
     * 分页查询（支持操作者、HTTP 方法、时间范围）。
     */
    public IPage<AdminAuditLog> page(int pageNum, int pageSize, String usernameKeyword, String actionKeyword,
                                     LocalDateTime start, LocalDateTime end) {
        return page(pageNum, pageSize, usernameKeyword, actionKeyword, null, null, null, start, end);
    }

    public IPage<AdminAuditLog> page(int pageNum, int pageSize, String usernameKeyword, String actionKeyword,
                                     String merchantId, String resourceType, String result,
                                     LocalDateTime start, LocalDateTime end) {
        return page(pageNum, pageSize, usernameKeyword, actionKeyword, merchantId, resourceType, result, start, end, null);
    }

    public IPage<AdminAuditLog> page(int pageNum, int pageSize, String usernameKeyword, String actionKeyword,
                                     String merchantId, String resourceType, String result,
                                     LocalDateTime start, LocalDateTime end, List<String> merchantScopeIds) {
        String scopedMerchantId = AdminRequestContext.resolveMerchantFilter(merchantId, merchantScopeIds);
        if ("__NO_ACCESS__".equals(scopedMerchantId)) {
            return new Page<>(pageNum, Math.min(Math.max(pageSize, 1), 100), 0);
        }
        LambdaQueryWrapper<AdminAuditLog> w = new LambdaQueryWrapper<>();
        if (usernameKeyword != null && !usernameKeyword.isBlank()) {
            w.like(AdminAuditLog::getUsername, usernameKeyword.trim());
        }
        if (actionKeyword != null && !actionKeyword.isBlank()) {
            w.eq(AdminAuditLog::getAction, actionKeyword.trim().toUpperCase());
        }
        if (StringUtils.hasText(scopedMerchantId)) {
            w.eq(AdminAuditLog::getMerchantId, scopedMerchantId);
        } else if (merchantScopeIds != null && !merchantScopeIds.isEmpty()) {
            w.in(AdminAuditLog::getMerchantId, merchantScopeIds);
        }
        if (resourceType != null && !resourceType.isBlank()) {
            w.eq(AdminAuditLog::getResourceType, resourceType.trim());
        }
        if (result != null && !result.isBlank()) {
            w.eq(AdminAuditLog::getResult, result.trim());
        }
        if (start != null) {
            w.ge(AdminAuditLog::getCreatedAt, start);
        }
        if (end != null) {
            w.le(AdminAuditLog::getCreatedAt, end);
        }
        w.orderByDesc(AdminAuditLog::getCreatedAt);
        Page<AdminAuditLog> p = new Page<>(pageNum, Math.min(Math.max(pageSize, 1), 100));
        return adminAuditLogMapper.selectPage(p, w);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
