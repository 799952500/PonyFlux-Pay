package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.cashier.SecurityAuditEntity;
import com.payflow.admin.mapper.cashier.SecurityAuditMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

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
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);

        LambdaQueryWrapper<SecurityAuditEntity> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(merchantId)) {
            wrapper.eq(SecurityAuditEntity::getMerchantId, merchantId.trim());
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
