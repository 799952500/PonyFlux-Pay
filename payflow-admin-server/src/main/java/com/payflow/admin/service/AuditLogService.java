package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.AdminAuditLog;
import com.payflow.admin.mapper.AdminAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
        AdminAuditLog row = new AdminAuditLog();
        row.setUsername(username != null ? username : "");
        row.setAction(action != null ? action : "");
        row.setResourcePath(resourcePath != null ? resourcePath : "");
        row.setDetail(truncate(detail, 1000));
        row.setClientIp(clientIp != null ? clientIp : "");
        row.setCreatedAt(LocalDateTime.now());
        adminAuditLogMapper.insert(row);
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
        LambdaQueryWrapper<AdminAuditLog> w = new LambdaQueryWrapper<>();
        if (usernameKeyword != null && !usernameKeyword.isBlank()) {
            w.like(AdminAuditLog::getUsername, usernameKeyword.trim());
        }
        if (actionKeyword != null && !actionKeyword.isBlank()) {
            w.eq(AdminAuditLog::getAction, actionKeyword.trim().toUpperCase());
        }
        if (start != null) {
            w.ge(AdminAuditLog::getCreatedAt, start);
        }
        if (end != null) {
            w.le(AdminAuditLog::getCreatedAt, end);
        }
        w.orderByDesc(AdminAuditLog::getCreatedAt);
        Page<AdminAuditLog> p = new Page<>(pageNum, pageSize);
        return adminAuditLogMapper.selectPage(p, w);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
