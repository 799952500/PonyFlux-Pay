package com.payflow.admin.service.recon;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.admin.dto.recon.ReconDiffCompleteRequest;
import com.payflow.admin.dto.recon.ReconLongTailSummaryDTO;
import com.payflow.admin.entity.recon.ReconDiffAssignmentEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.enums.recon.ReconDiffWorkflowStatusEnum;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.common.exception.BizException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 长尾差异统计与批量挂账。
 */
@Service
public class ReconLongTailService {

    private static final Set<String> OPEN = Set.of(
            ReconDiffWorkflowStatusEnum.UNASSIGNED.name(),
            ReconDiffWorkflowStatusEnum.ASSIGNED.name(),
            ReconDiffWorkflowStatusEnum.IN_PROGRESS.name(),
            ReconDiffWorkflowStatusEnum.ESCALATED.name());

    private final ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper;
    private final ReconDiffEntityMapper reconDiffEntityMapper;
    private final ReconDiffWorkflowService reconDiffWorkflowService;

    public ReconLongTailService(
            ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper,
            ReconDiffEntityMapper reconDiffEntityMapper,
            ReconDiffWorkflowService reconDiffWorkflowService) {
        this.reconDiffAssignmentEntityMapper = reconDiffAssignmentEntityMapper;
        this.reconDiffEntityMapper = reconDiffEntityMapper;
        this.reconDiffWorkflowService = reconDiffWorkflowService;
    }

    public ReconLongTailSummaryDTO buildSummary(LocalDate asOf, List<String> merchantScopeIds) {
        List<ReconDiffAssignmentEntity> open = reconDiffAssignmentEntityMapper.selectList(
                Wrappers.<ReconDiffAssignmentEntity>lambdaQuery()
                        .in(ReconDiffAssignmentEntity::getWorkflowStatus, OPEN));
        Map<String, ReconLongTailSummaryDTO.Bucket> buckets = new HashMap<>();
        initBuckets(buckets);
        int maxAge = 0;
        List<Long> diffIds = open.stream()
                .map(ReconDiffAssignmentEntity::getDiffId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ReconDiffEntity> diffById = diffIds.isEmpty()
                ? Map.of()
                : reconDiffEntityMapper.selectBatchIds(diffIds).stream()
                .collect(Collectors.toMap(ReconDiffEntity::getId, d -> d, (a, b) -> a));
        for (ReconDiffAssignmentEntity a : open) {
            if (!isMerchantAllowed(a.getMerchantId(), merchantScopeIds)) {
                continue;
            }
            int ageDays = (int) ChronoUnit.DAYS.between(a.getCreatedAt().toLocalDate(), asOf);
            maxAge = Math.max(maxAge, ageDays);
            String bucket = ageBucket(ageDays);
            ReconLongTailSummaryDTO.Bucket b = buckets.get(bucket);
            b.setDiffCount(b.getDiffCount() + 1);
            ReconDiffEntity diff = diffById.get(a.getDiffId());
            if (diff != null) {
                b.setDiffAmount(b.getDiffAmount() + diffAmount(diff));
            }
        }
        ReconLongTailSummaryDTO dto = new ReconLongTailSummaryDTO();
        dto.setBuckets(buckets.values().stream().toList());
        dto.setMaxAgeDays(maxAge);
        return dto;
    }

    @Transactional
    public void batchAcceptLoss(List<Long> diffIds, String remark, String operator, String clientIp,
                                List<String> merchantScopeIds) {
        if (!StringUtils.hasText(remark) || remark.trim().length() < 20) {
            throw new BizException(7567, "挂账原因至少 20 个字符");
        }
        if (diffIds.size() > 50) {
            throw new BizException(7570, "单次最多挂账 50 条");
        }
        ReconDiffCompleteRequest req = new ReconDiffCompleteRequest();
        req.setAction(ReconDiffWorkflowStatusEnum.ACCEPTED_LOSS.name());
        req.setRemark(remark.trim());
        for (Long diffId : diffIds) {
            ReconDiffAssignmentEntity a = reconDiffAssignmentEntityMapper.selectOne(
                    Wrappers.<ReconDiffAssignmentEntity>lambdaQuery()
                            .eq(ReconDiffAssignmentEntity::getDiffId, diffId));
            if (a == null) {
                throw new BizException(7560, "工单不存在: " + diffId);
            }
            if (merchantScopeIds != null
                    && (merchantScopeIds.isEmpty() || !merchantScopeIds.contains(a.getMerchantId()))) {
                throw new BizException(7503, "授权商户范围不包含目标资源");
            }
            reconDiffWorkflowService.completeAsManager(diffId, req, operator, clientIp);
        }
    }

    private static void initBuckets(Map<String, ReconLongTailSummaryDTO.Bucket> buckets) {
        for (String key : List.of("LT_1D", "D1_3", "D3_7", "D7_30", "GT_30")) {
            ReconLongTailSummaryDTO.Bucket b = new ReconLongTailSummaryDTO.Bucket();
            b.setAgeBucket(key);
            buckets.put(key, b);
        }
    }

    public static String ageBucket(int ageDays) {
        if (ageDays < 1) {
            return "LT_1D";
        }
        if (ageDays < 3) {
            return "D1_3";
        }
        if (ageDays < 7) {
            return "D3_7";
        }
        if (ageDays < 30) {
            return "D7_30";
        }
        return "GT_30";
    }

    private static long diffAmount(ReconDiffEntity d) {
        long ch = d.getChannelAmount() != null ? d.getChannelAmount() : 0L;
        long loc = d.getLocalAmount() != null ? d.getLocalAmount() : 0L;
        return Math.max(ch, loc);
    }

    private static boolean isMerchantAllowed(String merchantId, List<String> merchantScopeIds) {
        if (merchantScopeIds == null) {
            return true;
        }
        return StringUtils.hasText(merchantId) && merchantScopeIds.contains(merchantId);
    }
}
