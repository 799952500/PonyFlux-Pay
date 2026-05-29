package com.payflow.admin.service;

import com.payflow.admin.controller.AdminExportController;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("后台导出与批量操作商户隔离测试")
class AdminExportMerchantIsolationTest {

    @Mock
    private DashboardAggregationService dashboardAggregationService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("商户管理员导出授权外商户报表时不创建导出任务")
    void exportReportRejectsMerchantOutsideScope() {
        AdminExportController controller = new AdminExportController(dashboardAggregationService, notificationService);
        when(request.getAttribute("role")).thenReturn("ADMIN");
        when(request.getAttribute("dataMerchantIds")).thenReturn("M100001");

        ResponseEntity<Map<String, Object>> response = controller.createExportTask(
                request, "2026-05-01", "2026-05-21", "M100002");

        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertEquals("denied", data.get("status"));
        verifyNoInteractions(dashboardAggregationService);
    }

    @Test
    @DisplayName("单商户管理员未指定商户时自动限定到授权商户")
    void exportReportUsesSingleAuthorizedMerchantWhenAllRequested() {
        AdminExportController controller = new AdminExportController(dashboardAggregationService, notificationService);
        when(request.getAttribute("role")).thenReturn("ADMIN");
        when(request.getAttribute("dataMerchantIds")).thenReturn("M100001");
        when(dashboardAggregationService.queryMetrics(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), eq("day"), eq("M100001")))
                .thenReturn(Map.of("totalAmount", 100L, "totalCount", 1L));

        ResponseEntity<Map<String, Object>> response = controller.createExportTask(
                request, "2026-05-01", "2026-05-21", "ALL");

        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertEquals("processing", data.get("status"));
        assertEquals("M100001", data.get("merchantId"));
        verify(dashboardAggregationService).queryMetrics(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), eq("day"), eq("M100001"));
    }

    @Test
    @DisplayName("系统管理员保留全局导出能力")
    void exportReportKeepsAllScopeForSystemAdmin() {
        AdminExportController controller = new AdminExportController(dashboardAggregationService, notificationService);
        when(request.getAttribute("role")).thenReturn("SUPER_ADMIN");
        when(dashboardAggregationService.queryMetrics(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), eq("day"), eq("ALL")))
                .thenReturn(Map.of("totalAmount", 200L, "totalCount", 2L));

        ResponseEntity<Map<String, Object>> response = controller.createExportTask(
                request, "2026-05-01", "2026-05-21", "ALL");

        Map<?, ?> data = (Map<?, ?>) response.getBody().get("data");
        assertEquals("processing", data.get("status"));
        assertEquals("ALL", data.get("merchantId"));
        verify(dashboardAggregationService).queryMetrics(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), eq("day"), eq("ALL"));
    }
}
