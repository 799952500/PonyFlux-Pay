package com.payflow.admin.service.recon;

import com.payflow.admin.dto.recon.ReconDiffAggregationDTO;
import com.payflow.admin.entity.recon.ReconDiffAssignmentEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconAggregationService 集成测试")
class ReconAggregationServiceTest {

    @Mock
    private ReconTaskEntityMapper taskMapper;
    @Mock
    private ReconDiffEntityMapper diffMapper;
    @Mock
    private ReconDiffAssignmentEntityMapper assignmentMapper;

    @Test
    @DisplayName("矩阵按渠道与类型聚合")
    void matrixAggregation() {
        ReconAggregationService service = new ReconAggregationService(taskMapper, diffMapper, assignmentMapper);
        LocalDate day = LocalDate.now().minusDays(1);
        ReconTaskEntity task = new ReconTaskEntity();
        task.setTaskId("T1");
        task.setBillDate(day);
        task.setChannel("alipay");
        when(taskMapper.selectList(any())).thenReturn(List.of(task));

        ReconDiffEntity diff = new ReconDiffEntity();
        diff.setId(1L);
        diff.setTaskId("T1");
        diff.setDiffType("AMOUNT_MISMATCH");
        diff.setChannelAmount(100L);
        diff.setLocalAmount(90L);
        diff.setMerchantId("M100001");
        when(diffMapper.selectList(any())).thenReturn(List.of(diff));

        ReconDiffAssignmentEntity a = new ReconDiffAssignmentEntity();
        a.setDiffId(1L);
        a.setMerchantId("M100001");
        when(assignmentMapper.selectList(any())).thenReturn(List.of(a));

        ReconDiffAggregationDTO dto = service.buildDashboard(day, day, null, null, null);
        assertEquals(1, dto.getMatrix().size());
        assertEquals(1, dto.getMatrix().get(0).getDiffCount());
    }

    @Test
    @DisplayName("1 万条差异聚合 P95 口径：单次 buildDashboard ≤2s")
    void dashboardPerformanceUnderTenThousandDiffs() {
        ReconAggregationService service = new ReconAggregationService(taskMapper, diffMapper, assignmentMapper);
        LocalDate day = LocalDate.now().minusDays(15);
        ReconTaskEntity task = new ReconTaskEntity();
        task.setTaskId("T_PERF");
        task.setBillDate(day);
        task.setChannel("wechat");
        when(taskMapper.selectList(any())).thenReturn(List.of(task));

        int n = 10_000;
        List<ReconDiffEntity> diffs = new java.util.ArrayList<>(n);
        List<ReconDiffAssignmentEntity> assignments = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            ReconDiffEntity diff = new ReconDiffEntity();
            diff.setId((long) i + 1);
            diff.setTaskId("T_PERF");
            diff.setDiffType(i % 2 == 0 ? "AMOUNT_MISMATCH" : "STATUS_MISMATCH");
            diff.setChannelAmount(100L + i);
            diff.setLocalAmount(90L + i);
            diff.setMerchantId("M100001");
            diffs.add(diff);
            ReconDiffAssignmentEntity a = new ReconDiffAssignmentEntity();
            a.setDiffId((long) i + 1);
            a.setMerchantId("M100001");
            assignments.add(a);
        }
        when(diffMapper.selectList(any())).thenReturn(diffs);
        when(assignmentMapper.selectList(any())).thenReturn(assignments);

        long[] samples = new long[5];
        for (int i = 0; i < samples.length; i++) {
            long start = System.nanoTime();
            ReconDiffAggregationDTO dto = service.buildDashboard(day.minusDays(29), day, null, null, null);
            samples[i] = (System.nanoTime() - start) / 1_000_000;
            assertEquals(n, dto.getMatrix().stream().mapToLong(ReconDiffAggregationDTO.MatrixCell::getDiffCount).sum());
        }
        java.util.Arrays.sort(samples);
        long p95 = samples[samples.length - 1];
        assertTrue(p95 <= 2000, "P95 聚合耗时 " + p95 + "ms 超过 2000ms");
    }
}
