package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.AdminAuditLog;
import com.payflow.admin.mapper.AdminAuditLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("审计日志商户定位测试")
class AdminAuditMerchantTraceTest {

    @Mock
    private AdminAuditLogMapper adminAuditLogMapper;

    @Test
    @DisplayName("分页查询可按商户号筛选")
    void pageFiltersByMerchantId() {
        AuditLogService service = new AuditLogService(adminAuditLogMapper);
        Page<AdminAuditLog> page = new Page<>(1, 20);
        AdminAuditLog log = new AdminAuditLog();
        log.setMerchantId("M100001");
        page.setRecords(List.of(log));
        when(adminAuditLogMapper.selectPage(any(), any())).thenReturn(page);

        IPage<AdminAuditLog> result = service.page(
                1, 20, null, null, "M100001", null, null, null, null, List.of("M100001", "M100002"));

        assertEquals("M100001", result.getRecords().get(0).getMerchantId());
        assertEquals(1, result.getRecords().size());
    }
}
