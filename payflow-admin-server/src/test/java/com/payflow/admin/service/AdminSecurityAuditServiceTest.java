package com.payflow.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.payflow.admin.entity.cashier.SecurityAuditEntity;
import com.payflow.admin.mapper.cashier.SecurityAuditMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 安全审计分页查询单元测试。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSecurityAuditService 测试")
class AdminSecurityAuditServiceTest {

    @Mock
    private SecurityAuditMapper securityAuditMapper;

    @InjectMocks
    private AdminSecurityAuditService adminSecurityAuditService;

    @Test
    @DisplayName("pageSize 超过 100 时截断为 100")
    void capsPageSizeAt100() {
        when(securityAuditMapper.selectPage(any(), any())).thenReturn(new Page<>());

        adminSecurityAuditService.page(1, 500, null, null, null, null, null, null);

        ArgumentCaptor<Page<SecurityAuditEntity>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(securityAuditMapper).selectPage(pageCaptor.capture(), any());
        assertEquals(100, pageCaptor.getValue().getSize());
    }
}
