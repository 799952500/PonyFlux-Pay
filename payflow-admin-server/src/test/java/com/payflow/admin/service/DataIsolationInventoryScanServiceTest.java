package com.payflow.admin.service;

import com.payflow.admin.entity.DataIsolationCheck;
import com.payflow.admin.mapper.DataIsolationCheckMapper;
import com.payflow.admin.mapper.cashier.OrderMapper;
import com.payflow.admin.mapper.cashier.RefundMapper;
import com.payflow.admin.mapper.recon.ReconDiffEntityMapper;
import com.payflow.admin.mapper.recon.ReconTaskEntityMapper;
import com.payflow.admin.service.impl.DataIsolationInventoryScanServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("数据隔离扫描服务测试")
class DataIsolationInventoryScanServiceTest {

    @Mock
    private DataIsolationCheckMapper dataIsolationCheckMapper;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private RefundMapper refundMapper;
    @Mock
    private ReconTaskEntityMapper reconTaskEntityMapper;
    @Mock
    private ReconDiffEntityMapper reconDiffEntityMapper;

    @Test
    @DisplayName("全量扫描会刷新或插入检查项")
    void runFullScanUpdatesChecks() {
        when(orderMapper.selectCount(any())).thenReturn(0L);
        when(refundMapper.selectCount(any())).thenReturn(0L);
        when(reconTaskEntityMapper.selectCount(any())).thenReturn(1L);
        when(reconDiffEntityMapper.selectCount(any())).thenReturn(0L);
        when(dataIsolationCheckMapper.selectOne(any())).thenReturn(existing("CHK-RECON-TASK"));

        DataIsolationInventoryScanServiceImpl service = new DataIsolationInventoryScanServiceImpl(
                dataIsolationCheckMapper, orderMapper, refundMapper, reconTaskEntityMapper, reconDiffEntityMapper);

        int updated = service.runFullScan();

        assertTrue(updated >= 4);
        verify(dataIsolationCheckMapper, atLeast(4)).updateById(any(DataIsolationCheck.class));
    }

    private static DataIsolationCheck existing(String checkId) {
        DataIsolationCheck row = new DataIsolationCheck();
        row.setCheckId(checkId);
        row.setRemediationStatus("PENDING");
        return row;
    }
}
