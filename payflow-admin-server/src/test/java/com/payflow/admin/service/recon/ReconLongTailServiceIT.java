package com.payflow.admin.service.recon;

import com.payflow.admin.dto.recon.ReconLongTailSummaryDTO;
import com.payflow.admin.entity.recon.ReconDiffAssignmentEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconLongTailService 集成测试")
class ReconLongTailServiceIT {

    @Mock
    private ReconDiffAssignmentEntityMapper assignmentMapper;
    @Mock
    private ReconDiffEntityMapper diffMapper;
    @Mock
    private ReconDiffWorkflowService workflowService;

    @Test
    @DisplayName("bucket 统计包含 GT_30")
    void summaryHasBuckets() {
        ReconLongTailService service = new ReconLongTailService(assignmentMapper, diffMapper, workflowService);
        ReconDiffAssignmentEntity a = new ReconDiffAssignmentEntity();
        a.setDiffId(9L);
        a.setMerchantId("M100001");
        a.setWorkflowStatus("ASSIGNED");
        a.setCreatedAt(LocalDateTime.now().minusDays(40));
        when(assignmentMapper.selectList(any())).thenReturn(List.of(a));
        ReconDiffEntity d = new ReconDiffEntity();
        d.setId(9L);
        d.setChannelAmount(1000L);
        when(diffMapper.selectById(9L)).thenReturn(d);

        ReconLongTailSummaryDTO summary = service.buildSummary(LocalDate.now(), null);
        assertTrue(summary.getBuckets().stream().anyMatch(b -> "GT_30".equals(b.getAgeBucket())));
    }
}
