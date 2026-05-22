package com.payflow.admin.service;

import com.payflow.admin.dto.MerchantScopeDTO;
import com.payflow.admin.entity.AdminUser;
import com.payflow.admin.mapper.AdminUserMapper;
import com.payflow.admin.service.impl.AdminMerchantScopeServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@DisplayName("系统管理员跨商户范围测试")
class SystemAdminMerchantScopeTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    @Test
    @DisplayName("超级管理员解析为平台范围可跨商户筛选")
    void superAdminHasPlatformScope() {
        AdminMerchantScopeServiceImpl service = new AdminMerchantScopeServiceImpl(adminUserMapper);
        AdminUser user = new AdminUser();
        user.setRole("SUPER_ADMIN");
        user.setUsername("sysadmin");

        MerchantScopeDTO scope = service.resolve(user);

        assertTrue(scope.isPlatformAdmin());
        assertEquals("PLATFORM", scope.getScopeMode());
    }
}
