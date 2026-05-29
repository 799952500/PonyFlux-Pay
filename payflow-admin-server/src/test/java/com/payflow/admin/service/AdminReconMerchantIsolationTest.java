package com.payflow.admin.service;

import com.payflow.admin.dto.recon.ReconSummaryResponse;
import com.payflow.admin.entity.recon.ReconTaskEntity;
import com.payflow.admin.mapper.cashier.ReconCashierPaymentRow;
import com.payflow.admin.mapper.cashier.ReconCashierReportMapper;
import com.payflow.admin.mapper.recon.ReconDiffAssignmentEntityMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconHandlerAuditEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReconQueryService 商户隔离测试")
class AdminReconMerchantIsolationTest {

    @Mock
    private ReconTaskEntityMapper reconTaskEntityMapper;

    @Mock
    private ReconDiffEntityMapper reconDiffEntityMapper;

    @Mock
    private ReconCashierReportMapper reconCashierReportMapper;

    @Mock
    private ReconDiffAssignmentEntityMapper reconDiffAssignmentEntityMapper;

    @Mock
    private ReconHandlerAuditEntityMapper reconHandlerAuditEntityMapper;

    @Test
    @DisplayName("对账订单结果传入授权外商户时返回空页且不查询交易数据")
    void orderResultsReturnEmptyForMerchantOutsideScope() {
        AdminReconQueryService service = service();

        Map<String, Object> result = service.pageOrderResults(
                LocalDate.now(), null, "M100002", null, false, 1, 20, List.of("M100001"));

        assertEquals(0L, result.get("total"));
        assertEquals(List.of(), result.get("list"));
        verify(reconCashierReportMapper, never()).countSuccessPaymentsOnBillDate(any(), any(), any(), any());
    }

    @Test
    @DisplayName("单商户授权未传商户时自动限定该商户")
    void orderResultsUseSingleAuthorizedMerchantWhenMerchantNotRequested() {
        AdminReconQueryService service = service();
        ReconCashierPaymentRow row = new ReconCashierPaymentRow();
        row.setPaymentId("PAY-1");
        row.setOrderId("ORD-1");
        row.setMerchantId("M100001");
        row.setAmount(100L);
        when(reconTaskEntityMapper.selectList(any())).thenReturn(List.of(task("TASK-1")));
        when(reconDiffEntityMapper.selectList(any())).thenReturn(List.of());
        when(reconCashierReportMapper.countSuccessPaymentsOnBillDate(any(), any(), eq("M100001"), any()))
                .thenReturn(1L);
        when(reconCashierReportMapper.listSuccessPaymentsOnBillDate(any(), any(), eq("M100001"), any(), any(Long.class), any(Long.class)))
                .thenReturn(List.of(row));

        Map<String, Object> result = service.pageOrderResults(
                LocalDate.now(), null, null, null, false, 1, 20, List.of("M100001"));

        assertEquals(1L, result.get("total"));
        assertEquals(1, ((List<?>) result.get("list")).size());
    }

    @Test
    @DisplayName("异常明细过滤授权外商户结果")
    void anomaliesFilterRowsOutsideScope() {
        AdminReconQueryService service = service();
        when(reconDiffEntityMapper.countAbnormalByBillDate(any(), any(), any(), any())).thenReturn(1L);
        when(reconDiffEntityMapper.listAbnormalByBillDate(any(), any(), any(), any(), any(Long.class), any(Long.class)))
                .thenReturn(List.of());

        Map<String, Object> result = service.pageAnomalies(
                LocalDate.now(), null, null, null, 1, 20, List.of("M100001"));

        assertEquals(List.of(), result.get("list"));
    }

    @Test
    @DisplayName("空商户授权范围的对账汇总不返回本地交易汇总")
    void summaryWithEmptyMerchantScopeReturnsZeroLocalMetrics() {
        AdminReconQueryService service = service();
        when(reconTaskEntityMapper.selectList(any())).thenReturn(List.of());

        ReconSummaryResponse result = service.buildSummary(LocalDate.now(), null, null, List.of());

        assertEquals(0L, result.getTotalLocalAmountFen());
        assertEquals(0L, result.getTotalChannelBillAmountFen());
        assertEquals(0L, result.getPendingDiffCount());
        verify(reconCashierReportMapper).aggregateLocalSuccessByAccount(any(), any());
    }

    private AdminReconQueryService service() {
        return new AdminReconQueryService(
                reconTaskEntityMapper,
                reconDiffEntityMapper,
                reconDiffAssignmentEntityMapper,
                reconHandlerAuditEntityMapper,
                reconCashierReportMapper);
    }

    private static ReconTaskEntity task(String taskId) {
        ReconTaskEntity task = new ReconTaskEntity();
        task.setTaskId(taskId);
        task.setStatus("SUCCESS");
        return task;
    }
}
