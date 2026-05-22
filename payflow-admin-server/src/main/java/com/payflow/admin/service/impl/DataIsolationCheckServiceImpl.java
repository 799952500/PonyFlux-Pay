package com.payflow.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.dto.DataIsolationCheckDTO;
import com.payflow.admin.dto.DataIsolationCheckQueryDTO;
import com.payflow.admin.dto.MerchantScopeDTO;
import com.payflow.admin.entity.DataIsolationCheck;
import com.payflow.admin.mapper.DataIsolationCheckMapper;
import com.payflow.admin.service.DataIsolationCheckService;
import com.payflow.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DataIsolationCheckServiceImpl implements DataIsolationCheckService {

    private final DataIsolationCheckMapper dataIsolationCheckMapper;

    @Override
    public IPage<DataIsolationCheckDTO> page(DataIsolationCheckQueryDTO query, MerchantScopeDTO scope) {
        DataIsolationCheckQueryDTO q = query != null ? query : new DataIsolationCheckQueryDTO();
        int page = q.getPage() == null || q.getPage() < 1 ? 1 : q.getPage();
        int size = q.getSize() == null || q.getSize() < 1 ? 20 : Math.min(q.getSize(), 100);

        LambdaQueryWrapper<DataIsolationCheck> wrapper = new LambdaQueryWrapper<>();
        eqIfText(wrapper, DataIsolationCheck::getClassification, q.getClassification());
        eqIfText(wrapper, DataIsolationCheck::getRiskLevel, q.getRiskLevel());
        eqIfText(wrapper, DataIsolationCheck::getRemediationStatus, q.getRemediationStatus());
        eqIfText(wrapper, DataIsolationCheck::getTargetType, q.getTargetType());

        if (scope != null && scope.isPlatformAdmin()) {
            eqIfText(wrapper, DataIsolationCheck::getMerchantId, q.getMerchantId());
        } else if (scope != null && scope.hasMerchantScope()) {
            if (StringUtils.hasText(q.getMerchantId())) {
                String requestedMerchantId = q.getMerchantId().trim();
                if (!scope.getAuthorizedMerchantIds().contains(requestedMerchantId)) {
                    return new Page<>(page, size);
                }
                wrapper.eq(DataIsolationCheck::getMerchantId, requestedMerchantId);
            } else {
                wrapper.and(w -> w.in(DataIsolationCheck::getMerchantId, scope.getAuthorizedMerchantIds())
                        .or()
                        .isNull(DataIsolationCheck::getMerchantId));
            }
        } else {
            wrapper.isNull(DataIsolationCheck::getMerchantId);
        }
        wrapper.orderByDesc(DataIsolationCheck::getRiskLevel, DataIsolationCheck::getUpdatedAt);

        IPage<DataIsolationCheck> raw = dataIsolationCheckMapper.selectPage(new Page<>(page, size), wrapper);
        return raw.convert(this::toDto);
    }

    private static <T> void eqIfText(LambdaQueryWrapper<DataIsolationCheck> wrapper,
                                     com.baomidou.mybatisplus.core.toolkit.support.SFunction<DataIsolationCheck, T> column,
                                     String value) {
        if (StringUtils.hasText(value)) {
            wrapper.eq(column, value.trim());
        }
    }

    @Override
    public DataIsolationCheckDTO updateRemediation(String checkId, String remediationStatus, String decisionReason,
                                                   MerchantScopeDTO scope) {
        if (!StringUtils.hasText(checkId)) {
            throw new BizException(400, "checkId 不能为空");
        }
        DataIsolationCheck row = dataIsolationCheckMapper.selectOne(
                new LambdaQueryWrapper<DataIsolationCheck>().eq(DataIsolationCheck::getCheckId, checkId.trim()));
        if (row == null) {
            throw new BizException(404, "检查项不存在");
        }
        if (scope != null && !scope.isPlatformAdmin() && StringUtils.hasText(row.getMerchantId())) {
            if (!scope.getAuthorizedMerchantIds().contains(row.getMerchantId())) {
                throw new BizException(6101, "无权更新该检查项");
            }
        }
        row.setRemediationStatus(remediationStatus.trim());
        if (StringUtils.hasText(decisionReason)) {
            row.setDecisionReason(decisionReason.trim());
        }
        dataIsolationCheckMapper.updateById(row);
        return toDto(row);
    }

    private DataIsolationCheckDTO toDto(DataIsolationCheck row) {
        return DataIsolationCheckDTO.builder()
                .id(row.getId())
                .checkId(row.getCheckId())
                .targetType(row.getTargetType())
                .targetName(row.getTargetName())
                .classification(row.getClassification())
                .merchantFieldStatus(row.getMerchantFieldStatus())
                .riskLevel(row.getRiskLevel())
                .affectedEntries(row.getAffectedEntries())
                .remediationStatus(row.getRemediationStatus())
                .decisionReason(row.getDecisionReason())
                .merchantId(row.getMerchantId())
                .lastScannedAt(row.getLastScannedAt())
                .createdAt(row.getCreatedAt())
                .updatedAt(row.getUpdatedAt())
                .build();
    }
}
