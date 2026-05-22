package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.dto.DataIsolationCheckDTO;
import com.payflow.admin.dto.DataIsolationCheckQueryDTO;
import com.payflow.admin.dto.MerchantScopeDTO;
import com.payflow.admin.entity.DataIsolationCheck;
import com.payflow.admin.mapper.DataIsolationCheckMapper;
import com.payflow.admin.service.impl.DataIsolationCheckServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DataIsolationCheckService 测试")
class DataIsolationCheckServiceTest {

    @Mock
    private DataIsolationCheckMapper dataIsolationCheckMapper;

    @Test
    @DisplayName("pageSize 超过 100 时截断为 100")
    void capsPageSizeAt100() {
        DataIsolationCheckServiceImpl service = new DataIsolationCheckServiceImpl(dataIsolationCheckMapper);
        DataIsolationCheckQueryDTO query = new DataIsolationCheckQueryDTO();
        query.setSize(500);
        when(dataIsolationCheckMapper.selectPage(any(), any())).thenReturn(new Page<>());

        service.page(query, platformScope());

        ArgumentCaptor<Page<DataIsolationCheck>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(dataIsolationCheckMapper).selectPage(pageCaptor.capture(), any());
        assertEquals(100, pageCaptor.getValue().getSize());
    }

    @Test
    @DisplayName("系统管理员可按商户筛选隔离检查项")
    void platformAdminCanFilterByMerchantId() {
        DataIsolationCheckServiceImpl service = new DataIsolationCheckServiceImpl(dataIsolationCheckMapper);
        DataIsolationCheckQueryDTO query = new DataIsolationCheckQueryDTO();
        query.setMerchantId("M100002");
        Page<DataIsolationCheck> page = new Page<>();
        page.setRecords(List.of(check("CHK-1", "M100002")));
        when(dataIsolationCheckMapper.selectPage(any(), any())).thenReturn(page);

        IPage<DataIsolationCheckDTO> result = service.page(query, platformScope());

        assertEquals(1, result.getRecords().size());
        assertEquals("M100002", result.getRecords().get(0).getMerchantId());
    }

    @Test
    @DisplayName("商户管理员未传商户时仅返回授权商户和全局检查项")
    void merchantAdminWithoutRequestedMerchantSeesAuthorizedAndGlobalChecks() {
        DataIsolationCheckServiceImpl service = new DataIsolationCheckServiceImpl(dataIsolationCheckMapper);
        Page<DataIsolationCheck> page = new Page<>();
        page.setRecords(List.of(check("CHK-ORDER", "M100001"), check("CHK-SYS", null)));
        when(dataIsolationCheckMapper.selectPage(any(), any())).thenReturn(page);

        IPage<DataIsolationCheckDTO> result = service.page(new DataIsolationCheckQueryDTO(), merchantScope());

        assertEquals(Arrays.asList("M100001", null), result.getRecords().stream()
                .map(DataIsolationCheckDTO::getMerchantId)
                .toList());
    }

    @Test
    @DisplayName("商户管理员传入授权外商户时返回空页且不查询授权外数据")
    void merchantAdminRequestedMerchantOutsideScopeReturnsEmptyPage() {
        DataIsolationCheckServiceImpl service = new DataIsolationCheckServiceImpl(dataIsolationCheckMapper);
        DataIsolationCheckQueryDTO query = new DataIsolationCheckQueryDTO();
        query.setMerchantId("M100002");

        IPage<DataIsolationCheckDTO> result = service.page(query, merchantScope());

        assertEquals(0, result.getTotal());
        assertEquals(List.of(), result.getRecords());
    }

    @Test
    @DisplayName("实体映射为 DTO 时保留检查项关键字段")
    void convertsEntityToDto() {
        DataIsolationCheckServiceImpl service = new DataIsolationCheckServiceImpl(dataIsolationCheckMapper);
        LocalDateTime now = LocalDateTime.now();
        DataIsolationCheck row = check("CHK-ORDER", "M100001");
        row.setId(9L);
        row.setLastScannedAt(now);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        Page<DataIsolationCheck> page = new Page<>();
        page.setRecords(List.of(row));
        when(dataIsolationCheckMapper.selectPage(any(), any())).thenReturn(page);

        DataIsolationCheckDTO dto = service.page(new DataIsolationCheckQueryDTO(), platformScope()).getRecords().get(0);

        assertEquals(9L, dto.getId());
        assertEquals("CHK-ORDER", dto.getCheckId());
        assertEquals("M100001", dto.getMerchantId());
        assertNotNull(dto.getLastScannedAt());
    }

    private static MerchantScopeDTO platformScope() {
        return MerchantScopeDTO.builder()
                .platformAdmin(true)
                .authorizedMerchantIds(List.of())
                .build();
    }

    private static MerchantScopeDTO merchantScope() {
        return MerchantScopeDTO.builder()
                .platformAdmin(false)
                .authorizedMerchantIds(List.of("M100001"))
                .build();
    }

    private static DataIsolationCheck check(String checkId, String merchantId) {
        DataIsolationCheck row = new DataIsolationCheck();
        row.setCheckId(checkId);
        row.setTargetType("DATA_TABLE");
        row.setTargetName("cashier_orders");
        row.setClassification("MERCHANT");
        row.setMerchantFieldStatus("PRESENT");
        row.setRiskLevel("HIGH");
        row.setAffectedEntries("订单列表");
        row.setRemediationStatus("PENDING");
        row.setDecisionReason("商户级数据");
        row.setMerchantId(merchantId);
        return row;
    }
}
