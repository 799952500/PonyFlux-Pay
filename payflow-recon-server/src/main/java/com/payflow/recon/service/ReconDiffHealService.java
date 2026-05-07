package com.payflow.recon.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.payflow.recon.entity.ReconDiff;
import com.payflow.recon.mapper.ReconDiffMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 对账差异自动标注建议动作（不自动改账，仅写入 suggested_action 供工作台处理）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReconDiffHealService {

    private final ReconDiffMapper reconDiffMapper;

    /**
     * 为本任务全部差异写入建议动作。
     */
    @Transactional(transactionManager = "adminTransactionManager")
    public void annotateSuggestions(String taskId) {
        List<ReconDiff> list = reconDiffMapper.selectList(
                Wrappers.<ReconDiff>lambdaQuery().eq(ReconDiff::getTaskId, taskId));
        for (ReconDiff d : list) {
            String suggestion = switch (d.getDiffType() != null ? d.getDiffType() : "") {
                case ReconCompareService.DIFF_STATUS_MISMATCH -> "AUTO_QUERY";
                case ReconCompareService.DIFF_LOCAL_ONLY -> "CHANNEL_QUERY_THEN_REVIEW";
                case ReconCompareService.DIFF_CHANNEL_ONLY -> "MANUAL_IMPORT_OR_REVIEW";
                case ReconCompareService.DIFF_AMOUNT_MISMATCH -> "MANUAL_REVIEW";
                default -> "REVIEW";
            };
            d.setSuggestedAction(suggestion);
            reconDiffMapper.updateById(d);
        }
        log.info("对账差异建议已标注: taskId={}, count={}", taskId, list.size());
    }
}
