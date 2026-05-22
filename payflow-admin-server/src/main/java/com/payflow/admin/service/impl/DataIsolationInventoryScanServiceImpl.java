package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.payflow.admin.entity.DataIsolationCheck;
import com.payflow.admin.entity.cashier.Order;
import com.payflow.admin.entity.cashier.Refund;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.mapper.DataIsolationCheckMapper;
import com.payflow.admin.mapper.cashier.OrderMapper;
import com.payflow.admin.mapper.cashier.RefundMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import com.payflow.admin.service.DataIsolationInventoryScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 扫描关键数据表并刷新隔离检查项。
 */
@Service
@RequiredArgsConstructor
public class DataIsolationInventoryScanServiceImpl implements DataIsolationInventoryScanService {

    private final DataIsolationCheckMapper dataIsolationCheckMapper;
    private final OrderMapper orderMapper;
    private final RefundMapper refundMapper;
    private final ReconTaskEntityMapper reconTaskEntityMapper;
    private final ReconDiffEntityMapper reconDiffEntityMapper;

    @Override
    @Transactional(transactionManager = "adminTransactionManager")
    public int runFullScan() {
        int updated = 0;
        updated += refreshCheck("CHK-CASHIER-ORDERS", countOrdersMissingMerchant(), "MERCHANT", "PRESENT");
        updated += refreshCheck("CHK-ADMIN-PAYMENTS", 0L, "MERCHANT", "PENDING_CONFIRM");
        updated += refreshCheck("CHK-ADMIN-REFUNDS", countRefundsNeedingMerchantInfer(), "MERCHANT", "PENDING_CONFIRM");
        updated += refreshCheck("CHK-RECON-TASK", countReconTasksMissingMerchant(), "MANUAL_REVIEW", "PENDING_CONFIRM");
        updated += refreshCheck("CHK-RECON-DIFF", countReconDiffsMissingMerchant(), "MANUAL_REVIEW", "PENDING_CONFIRM");
        updated += refreshCheck("CHK-ADMIN-SYS-CONFIG", 0L, "GLOBAL", "PRESENT");
        updated += refreshCheck("CHK-ADMIN-AUDIT", 0L, "SYSTEM_AUDIT", "PRESENT");
        return updated;
    }

    private long countOrdersMissingMerchant() {
        return orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .and(w -> w.isNull(Order::getMerchantId).or().eq(Order::getMerchantId, "")));
    }

    private long countRefundsNeedingMerchantInfer() {
        return refundMapper.selectCount(null);
    }

    private long countReconTasksMissingMerchant() {
        return reconTaskEntityMapper.selectCount(new LambdaQueryWrapper<ReconTaskEntity>()
                .and(w -> w.isNull(ReconTaskEntity::getMerchantId).or().eq(ReconTaskEntity::getMerchantId, "")));
    }

    private long countReconDiffsMissingMerchant() {
        return reconDiffEntityMapper.selectCount(new LambdaQueryWrapper<ReconDiffEntity>()
                .and(w -> w.isNull(ReconDiffEntity::getMerchantId).or().eq(ReconDiffEntity::getMerchantId, "")));
    }

    private int refreshCheck(String checkId, long affectedCount, String classification, String fieldStatus) {
        DataIsolationCheck existing = dataIsolationCheckMapper.selectOne(
                new LambdaQueryWrapper<DataIsolationCheck>().eq(DataIsolationCheck::getCheckId, checkId));
        String risk = affectedCount > 0 ? "HIGH" : "LOW";
        String remediation = affectedCount > 0 ? "NEEDS_MANUAL_REVIEW" : "DONE";
        String summary = "扫描样本数=" + affectedCount;
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            DataIsolationCheck row = new DataIsolationCheck();
            row.setCheckId(checkId);
            row.setTargetType("DATA_TABLE");
            row.setTargetName(checkId);
            row.setClassification(classification);
            row.setMerchantFieldStatus(fieldStatus);
            row.setRiskLevel(risk);
            row.setAffectedEntries(summary);
            row.setRemediationStatus(remediation);
            row.setDecisionReason("自动扫描生成");
            row.setLastScannedAt(now);
            dataIsolationCheckMapper.insert(row);
            return 1;
        }
        existing.setClassification(classification);
        existing.setMerchantFieldStatus(fieldStatus);
        existing.setRiskLevel(risk);
        existing.setAffectedEntries(summary);
        if (affectedCount == 0 && !"EXEMPTED".equals(existing.getRemediationStatus())) {
            existing.setRemediationStatus("DONE");
        } else if (affectedCount > 0) {
            existing.setRemediationStatus("NEEDS_MANUAL_REVIEW");
        }
        existing.setLastScannedAt(now);
        dataIsolationCheckMapper.updateById(existing);
        return 1;
    }
}
