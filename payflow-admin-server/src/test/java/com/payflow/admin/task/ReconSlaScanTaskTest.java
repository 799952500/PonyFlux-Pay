package com.payflow.admin.task;

import com.payflow.admin.entity.recon.ReconDiffAssignmentEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconDiffSlaRuleEntity;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffSlaRuleEntityMapper;
import com.payflow.admin.service.NotificationService;
import com.payflow.admin.service.recon.ReconAuditService;
import com.payflow.admin.service.recon.ReconSlaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconSlaService 扫描集成测试")
class ReconSlaScanTaskTest {

    @Mock
    private ReconDiffSlaRuleEntityMapper slaRuleMapper;
    @Mock
    private ReconDiffAssignmentEntityMapper assignmentMapper;
    @Mock
    private ReconDiffEntityMapper diffMapper;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ReconAuditService reconAuditService;

    @Test
    @DisplayName("overdue 仅升级一次")
    void overdueEscalatesOnce() {
        ReconSlaService service = new ReconSlaService(
                slaRuleMapper, assignmentMapper, diffMapper, notificationService, reconAuditService);
        ReconDiffAssignmentEntity a = new ReconDiffAssignmentEntity();
        a.setDiffId(1L);
        a.setWorkflowStatus("ASSIGNED");
        a.setCreatedAt(LocalDateTime.now().minusHours(5));
        a.setDueAt(LocalDateTime.now().minusMinutes(5));
        when(assignmentMapper.selectList(any())).thenReturn(List.of(a));

        ReconDiffEntity diff = new ReconDiffEntity();
        diff.setId(1L);
        diff.setDiffType("AMOUNT_MISMATCH");
        diff.setMerchantId("M100001");
        when(diffMapper.selectBatchIds(any())).thenReturn(List.of(diff));

        ReconDiffSlaRuleEntity rule = new ReconDiffSlaRuleEntity();
        rule.setDiffType("AMOUNT_MISMATCH");
        rule.setEnabled(1);
        rule.setDueSoonRatio(new BigDecimal("0.2"));
        rule.setEscalateToRole("recon:manage");
        when(slaRuleMapper.selectList(any())).thenReturn(List.of(rule));

        service.scanDueSoonAndOverdue();

        verify(assignmentMapper, atMostOnce())
                .updateById(org.mockito.ArgumentMatchers.<ReconDiffAssignmentEntity>any());
        verify(reconAuditService, atMostOnce()).record(any(Long.class), any(), any(), any(), any());
    }
}
