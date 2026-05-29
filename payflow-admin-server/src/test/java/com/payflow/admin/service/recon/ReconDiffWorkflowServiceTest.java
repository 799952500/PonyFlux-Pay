package com.payflow.admin.service.recon;

import com.payflow.admin.dto.recon.ReconDiffAssignRequest;
import com.payflow.admin.dto.recon.ReconDiffCompleteRequest;
import com.payflow.admin.dto.recon.ReconDiffStartRequest;
import com.payflow.admin.entity.recon.ReconDiffAssignmentEntity;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReconDiffWorkflowService 状态机测试")
class ReconDiffWorkflowServiceTest {

    @Mock
    private ReconDiffEntityMapper reconDiffEntityMapper;

    @Mock
    private ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper;

    @Mock
    private ReconAuditService reconAuditService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReconSlaService reconSlaService;

    @Test
    @DisplayName("未指派工单才能认领")
    void claimOnlyWhenUnassigned() {
        ReconDiffWorkflowService service = service();
        ReconDiffEntity diff = diff(1L);
        ReconDiffAssignmentEntity a = assignment(1L, "M100001", "ASSIGNED", null);
        when(reconDiffEntityMapper.selectById(1L)).thenReturn(diff);
        when(reconDiffAssignmentEntityMapper.selectOne(any())).thenReturn(a);

        assertThrows(RuntimeException.class, () -> service.claim(1L, "admin", "127.0.0.1"));
    }

    @Test
    @DisplayName("已指派或已升级工单允许开始处理，进入 IN_PROGRESS")
    void startFromAssignedToInProgress() {
        ReconDiffWorkflowService service = service();
        ReconDiffEntity diff = diff(2L);
        ReconDiffAssignmentEntity a = assignment(2L, "M100001", "ASSIGNED", "admin");
        when(reconDiffEntityMapper.selectById(2L)).thenReturn(diff);
        when(reconDiffAssignmentEntityMapper.selectOne(any())).thenReturn(a);

        ReconDiffStartRequest req = new ReconDiffStartRequest();
        req.setRemark("开始处理");
        service.start(2L, req, "admin", "127.0.0.1");

        assertEquals("IN_PROGRESS", a.getWorkflowStatus());
    }

    @Test
    @DisplayName("完成 PROCESSED 需要至少 10 字说明")
    void completeProcessedNeedsMinRemark() {
        ReconDiffWorkflowService service = service();
        ReconDiffEntity diff = diff(3L);
        ReconDiffAssignmentEntity a = assignment(3L, "M100001", "IN_PROGRESS", "admin");
        when(reconDiffEntityMapper.selectById(3L)).thenReturn(diff);
        when(reconDiffAssignmentEntityMapper.selectOne(any())).thenReturn(a);

        ReconDiffCompleteRequest req = new ReconDiffCompleteRequest();
        req.setAction("PROCESSED");
        req.setRemark("太短");
        assertThrows(RuntimeException.class, () -> service.complete(3L, req, "admin", "127.0.0.1"));
    }

    @Test
    @DisplayName("指派后工作流状态进入 ASSIGNED")
    void assignSetsAssigned() {
        ReconDiffWorkflowService service = service();
        ReconDiffEntity diff = diff(4L);
        ReconDiffAssignmentEntity a = assignment(4L, "M100001", "UNASSIGNED", null);
        when(reconDiffEntityMapper.selectById(4L)).thenReturn(diff);
        when(reconDiffAssignmentEntityMapper.selectOne(any())).thenReturn(a);

        ReconDiffAssignRequest req = new ReconDiffAssignRequest();
        req.setAssigneeId("finance_demo");
        req.setRemark("请处理");
        service.assign(4L, req, "admin", "127.0.0.1");

        assertEquals("ASSIGNED", a.getWorkflowStatus());
        assertEquals("finance_demo", a.getAssigneeId());
    }

    private ReconDiffWorkflowService service() {
        return new ReconDiffWorkflowService(
                reconDiffEntityMapper,
                reconDiffAssignmentEntityMapper,
                reconAuditService,
                notificationService,
                reconSlaService);
    }

    private static ReconDiffEntity diff(Long id) {
        ReconDiffEntity d = new ReconDiffEntity();
        d.setId(id);
        d.setTaskId("RECON-20260517-001");
        d.setMerchantId("M100001");
        d.setDiffType("AMOUNT_MISMATCH");
        d.setHandleStatus("PENDING");
        d.setCreatedAt(LocalDateTime.now());
        return d;
    }

    private static ReconDiffAssignmentEntity assignment(Long diffId, String merchantId, String status, String assignee) {
        ReconDiffAssignmentEntity a = new ReconDiffAssignmentEntity();
        a.setId(100L + diffId);
        a.setDiffId(diffId);
        a.setMerchantId(merchantId);
        a.setWorkflowStatus(status);
        a.setAssigneeId(assignee);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        return a;
    }
}

