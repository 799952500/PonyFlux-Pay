package com.payflow.admin.controller;

import com.payflow.admin.dto.recon.ReconDiffAssignRequest;
import com.payflow.admin.dto.recon.ReconDiffCommentRequest;
import com.payflow.admin.dto.recon.ReconDiffCompleteRequest;
import com.payflow.admin.dto.recon.ReconDiffStartRequest;
import com.payflow.admin.entity.recon.ReconDiffEntity;
import com.payflow.admin.entity.recon.ReconHandlerAuditEntity;
import com.payflow.admin.entity.recon.ReconMerchantTaskEntity;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconHandlerAuditEntityMapper;
import com.payflow.admin.mapper.recon.ReconMerchantTaskEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import com.payflow.admin.service.AdminReconQueryService;
import com.payflow.admin.service.NotificationService;
import com.payflow.admin.service.recon.ReconDiffWorkflowService;
import com.payflow.admin.service.recon.ReconAggregationService;
import com.payflow.admin.service.recon.ReconLongTailService;
import com.payflow.admin.service.recon.ReconReportService;
import com.payflow.admin.service.recon.ReconSlaService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReconController 工单接口编排测试")
class AdminReconWorkItemTest {

    @Mock
    private ReconTaskEntityMapper reconTaskEntityMapper;
    @Mock
    private ReconMerchantTaskEntityMapper reconMerchantTaskEntityMapper;
    @Mock
    private ReconDiffEntityMapper reconDiffEntityMapper;
    @Mock
    private ReconHandlerAuditEntityMapper reconHandlerAuditEntityMapper;
    @Mock
    private AdminReconQueryService adminReconQueryService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private ReconDiffWorkflowService reconDiffWorkflowService;
    @Mock
    private ReconSlaService reconSlaService;
    @Mock
    private ReconAggregationService reconAggregationService;
    @Mock
    private ReconLongTailService reconLongTailService;
    @Mock
    private ReconReportService reconReportService;
    @Mock
    private HttpServletRequest http;

    @Test
    @DisplayName("认领接口应调用工作流服务并返回 success")
    void claimShouldCallWorkflowService() {
        AdminReconController controller = controller();
        when(http.getAttribute("username")).thenReturn("admin");
        when(http.getRemoteAddr()).thenReturn("127.0.0.1");

        ResponseEntity<Map<String, Object>> resp = controller.claim(http, 1L);

        assertEquals(0, resp.getBody().get("code"));
        verify(reconDiffWorkflowService).claim(1L, "admin", "127.0.0.1");
    }

    @Test
    @DisplayName("详情接口应委托查询服务并返回数据")
    void detailShouldDelegateQueryService() {
        AdminReconController controller = controller();
        when(adminReconQueryService.getWorkItemDetail(eq(2L), any())).thenReturn(
                Map.of("diff", new ReconDiffEntity(), "assignment", Map.of(), "audits", List.of()));

        ResponseEntity<Map<String, Object>> resp = controller.workItemDetail(http, 2L);

        assertEquals(0, resp.getBody().get("code"));
        Map<?, ?> data = (Map<?, ?>) resp.getBody().get("data");
        assertEquals(true, data.containsKey("diff"));
    }

    @Test
    @DisplayName("终态接口应调用工作流服务")
    void completeShouldCallWorkflowService() {
        AdminReconController controller = controller();
        when(http.getAttribute("username")).thenReturn("admin");
        when(http.getRemoteAddr()).thenReturn("127.0.0.1");
        ReconDiffCompleteRequest req = new ReconDiffCompleteRequest();
        req.setAction("PROCESSED");
        req.setRemark("这是一个足够长的处置说明");

        ResponseEntity<Map<String, Object>> resp = controller.complete(http, 3L, req);

        assertEquals(0, resp.getBody().get("code"));
        verify(reconDiffWorkflowService).complete(3L, req, "admin", "127.0.0.1");
    }

    private AdminReconController controller() {
        return new AdminReconController(
                reconTaskEntityMapper,
                reconMerchantTaskEntityMapper,
                reconDiffEntityMapper,
                reconHandlerAuditEntityMapper,
                adminReconQueryService,
                notificationService,
                reconDiffWorkflowService,
                reconSlaService,
                reconAggregationService,
                reconLongTailService,
                reconReportService);
    }
}

