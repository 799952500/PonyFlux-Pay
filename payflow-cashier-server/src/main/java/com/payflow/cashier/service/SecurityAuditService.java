package com.payflow.cashier.service;

import com.payflow.cashier.context.AuthMode;
import com.payflow.cashier.entity.SecurityAuditEntity;
import com.payflow.cashier.mapper.SecurityAuditMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 安全审计异步写入。
 *
 * @author PayFlow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityAuditService {

    private final SecurityAuditMapper securityAuditMapper;

    /**
     * 记录越权拒绝事件（异步，失败仅打日志）。
     */
    @Async("securityAuditExecutor")
    public void recordDenied(
            String merchantId,
            String targetMerchantId,
            AuthMode authMode,
            String httpMethod,
            String requestPath,
            String resourceType,
            String resourceId,
            String clientIp,
            String userAgent,
            int reasonCode,
            String reasonDetail) {
        try {
            SecurityAuditEntity row = SecurityAuditEntity.builder()
                    .merchantId(merchantId != null ? merchantId : "")
                    .targetMerchantId(targetMerchantId)
                    .authMode(authMode != null ? authMode.name() : "UNKNOWN")
                    .httpMethod(httpMethod)
                    .requestPath(requestPath)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .clientIp(clientIp)
                    .userAgent(truncate(userAgent, 512))
                    .outcome("DENIED")
                    .reasonCode(String.valueOf(reasonCode))
                    .reasonDetail(truncate(reasonDetail, 512))
                    .createdAt(LocalDateTime.now())
                    .build();
            securityAuditMapper.insert(row);
        } catch (Exception e) {
            log.error("安全审计写入失败: path={}, reasonCode={}", requestPath, reasonCode, e);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
