package com.payflow.admin.service.recon;

import com.payflow.admin.entity.recon.ReconHandlerAuditEntity;
import com.payflow.admin.mapper.recon.ReconHandlerAuditEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 对账差异工单审计写入封装。
 */
@Service
@RequiredArgsConstructor
public class ReconAuditService {

    private final ReconHandlerAuditEntityMapper reconHandlerAuditEntityMapper;

    public void record(long diffId, String action, String operator, String detail, String clientIp) {
        ReconHandlerAuditEntity audit = new ReconHandlerAuditEntity();
        audit.setDiffId(diffId);
        audit.setAction(action);
        audit.setOperator(operator);
        audit.setDetail(detail);
        audit.setClientIp(clientIp);
        audit.setCreatedAt(LocalDateTime.now());
        reconHandlerAuditEntityMapper.insert(audit);
    }
}

