package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.cashier.SecurityAuditEntity;
import com.payflow.admin.kit.AdminRequestContext;
import com.payflow.admin.mapper.cashier.SecurityAuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全审计分页查询。
 *
 * @author PayFlow Team
 */
@Service
@RequiredArgsConstructor
public class AdminSecurityAuditService {

    private static final int MAX_PAGE_SIZE = 100;

    private final SecurityAuditMapper securityAuditMapper;

    public IPage<SecurityAuditEntity> page(int page, int pageSize, String merchantId, String outcome,
                                           String reasonCode, String requestPath,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        return page(page, pageSize, merchantId, outcome, reasonCode, requestPath, startTime, endTime, null);
    }

    public IPage<SecurityAuditEntity> page(int page, int pageSize, String merchantId, String outcome,
                                           String reasonCode, String requestPath,
                                           LocalDateTime startTime, LocalDateTime endTime,
                                           List<String> merchantScopeIds) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        String scopedMerchantId = AdminRequestContext.resolveMerchantFilter(merchantId, merchantScopeIds);
        if ("__NO_ACCESS__".equals(scopedMerchantId)) {
            return new Page<>(safePage, safeSize, 0);
        }

        LambdaQueryWrapper<SecurityAuditEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(scopedMerchantId)) {
            wrapper.eq(SecurityAuditEntity::getMerchantId, scopedMerchantId);
        } else if (merchantScopeIds != null && !merchantScopeIds.isEmpty()) {
            wrapper.in(SecurityAuditEntity::getMerchantId, merchantScopeIds);
        }
        if (StringUtils.hasText(outcome)) {
            wrapper.eq(SecurityAuditEntity::getOutcome, outcome.trim());
        }
        if (StringUtils.hasText(reasonCode)) {
            wrapper.eq(SecurityAuditEntity::getReasonCode, reasonCode.trim());
        }
        if (StringUtils.hasText(requestPath)) {
            wrapper.like(SecurityAuditEntity::getRequestPath, requestPath.trim());
        }
        if (startTime != null) {
            wrapper.ge(SecurityAuditEntity::getCreatedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(SecurityAuditEntity::getCreatedAt, endTime);
        }
        wrapper.orderByDesc(SecurityAuditEntity::getCreatedAt);
        return securityAuditMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
    }
}
